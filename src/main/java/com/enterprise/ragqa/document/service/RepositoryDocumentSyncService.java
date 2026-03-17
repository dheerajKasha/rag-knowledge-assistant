package com.enterprise.ragqa.document.service;

import com.enterprise.ragqa.config.DocumentRepositoryProperties;
import com.enterprise.ragqa.document.model.DocumentRecord;
import com.enterprise.ragqa.document.repository.DocumentRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryDocumentSyncService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RepositoryDocumentSyncService.class);

    private final DocumentRepositoryProperties properties;
    private final DocumentRepository documentRepository;
    private final DocumentIngestionService documentIngestionService;
    private final DocumentHashingService documentHashingService;
    private final TextExtractionService textExtractionService;

    public RepositoryDocumentSyncService(
            DocumentRepositoryProperties properties,
            DocumentRepository documentRepository,
            DocumentIngestionService documentIngestionService,
            DocumentHashingService documentHashingService,
            TextExtractionService textExtractionService
    ) {
        this.properties = properties;
        this.documentRepository = documentRepository;
        this.documentIngestionService = documentIngestionService;
        this.documentHashingService = documentHashingService;
        this.textExtractionService = textExtractionService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.startupRefreshEnabled()) {
            return;
        }

        IndexRefreshResult result = refreshRepositoryDocuments();
        log.info(
                "Repository document refresh completed. indexed={}, updated={}, removed={}, path={}",
                result.indexedCount(),
                result.updatedCount(),
                result.removedCount(),
                repositoryPath()
        );
    }

    @Transactional
    public IndexRefreshResult refreshRepositoryDocuments() throws IOException {
        Path root = repositoryPath();
        Files.createDirectories(root);

        List<DocumentRecord> indexedDocuments = documentRepository.findBySourceType(DocumentSourceType.REPOSITORY);
        Set<String> seenPaths = new HashSet<>();
        int indexedCount = 0;
        int updatedCount = 0;

        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(current -> textExtractionService.supports(current.getFileName().toString()))
                    .toList()) {
                String normalizedPath = root.relativize(path).toString().replace('\\', '/');
                ExtractedDocumentContent extractedContent = textExtractionService.extract(path);
                if (extractedContent.text().isBlank()) {
                    continue;
                }

                seenPaths.add(normalizedPath);
                String contentHash = documentHashingService.sha256(extractedContent.text());
                var existing = documentRepository.findBySourceTypeAndSourcePath(DocumentSourceType.REPOSITORY, normalizedPath);
                if (existing.isPresent() && contentHash.equals(existing.get().getContentHash())) {
                    continue;
                }

                if (existing.isPresent()) {
                    documentIngestionService.reindexRepositoryDocument(path, normalizedPath, contentHash, true);
                    updatedCount++;
                } else {
                    documentIngestionService.reindexRepositoryDocument(path, normalizedPath, contentHash, false);
                    indexedCount++;
                }
            }
        }

        int removedCount = 0;
        for (DocumentRecord record : indexedDocuments) {
            String sourcePath = record.getSourcePath();
            if (sourcePath != null && !seenPaths.contains(sourcePath)) {
                documentIngestionService.removeDocument(record.getId());
                removedCount++;
            }
        }

        return new IndexRefreshResult(indexedCount, updatedCount, removedCount);
    }

    public Path repositoryPath() {
        return Path.of(properties.repoPath());
    }
}
