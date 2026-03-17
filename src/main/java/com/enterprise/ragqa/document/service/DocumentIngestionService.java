package com.enterprise.ragqa.document.service;

import com.enterprise.ragqa.api.dto.DocumentSummaryDto;
import com.enterprise.ragqa.api.dto.DocumentUploadResponse;
import com.enterprise.ragqa.document.model.DocumentChunkRecord;
import com.enterprise.ragqa.document.model.DocumentRecord;
import com.enterprise.ragqa.document.repository.DocumentChunkRepository;
import com.enterprise.ragqa.document.repository.DocumentRepository;
import com.enterprise.ragqa.embedding.EmbeddingClient;
import com.enterprise.ragqa.embedding.VectorMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionService {

    private final TextExtractionService textExtractionService;
    private final ChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentHashingService documentHashingService;

    public DocumentIngestionService(
            TextExtractionService textExtractionService,
            ChunkingService chunkingService,
            EmbeddingClient embeddingClient,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentHashingService documentHashingService
    ) {
        this.textExtractionService = textExtractionService;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentHashingService = documentHashingService;
    }

    @Transactional
    public DocumentUploadResponse ingest(MultipartFile file, String uploadedBy) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        ExtractedDocumentContent extractedContent = textExtractionService.extract(file);
        List<ChunkCandidate> chunks = chunkingService.chunk(extractedContent);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("No readable text was extracted from the document.");
        }

        DocumentRecord document = persistDocument(
                file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename(),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                uploadedBy,
                null,
                DocumentSourceType.UPLOAD,
                documentHashingService.sha256(extractedContent.text()),
                extractedContent.text(),
                chunks
        );

        return new DocumentUploadResponse(document.getId(), document.getFilename(), chunks.size());
    }

    @Transactional
    public void reindexRepositoryDocument(Path path, String sourcePath, String contentHash, boolean replacingExisting) throws IOException {
        if (replacingExisting) {
            documentRepository.findBySourceTypeAndSourcePath(DocumentSourceType.REPOSITORY, sourcePath)
                    .ifPresent(existing -> removeDocument(existing.getId()));
        }

        ExtractedDocumentContent extractedContent = textExtractionService.extract(path);
        List<ChunkCandidate> chunks = chunkingService.chunk(extractedContent);
        if (chunks.isEmpty()) {
            return;
        }

        persistDocument(
                path.getFileName().toString(),
                contentTypeFromFilename(path.getFileName().toString()),
                "repository-scanner",
                sourcePath,
                DocumentSourceType.REPOSITORY,
                contentHash,
                extractedContent.text(),
                chunks
        );
    }

    @Transactional
    public void removeDocument(UUID documentId) {
        documentChunkRepository.deleteByDocument_Id(documentId);
        documentRepository.deleteById(documentId);
    }

    @Transactional(readOnly = true)
    public List<DocumentSummaryDto> listDocuments() {
        return documentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(document -> new DocumentSummaryDto(
                        document.getId(),
                        document.getFilename(),
                        document.getUploadedBy(),
                        document.getContentType(),
                        Math.toIntExact(documentChunkRepository.countByDocument_Id(document.getId())),
                        document.getCreatedAt()
                ))
                .toList();
    }

    private DocumentRecord persistDocument(
            String filename,
            String contentType,
            String uploadedBy,
            String sourcePath,
            String sourceType,
            String contentHash,
            String extractedText,
            List<ChunkCandidate> chunks
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        DocumentRecord document = documentRepository.save(new DocumentRecord(
                UUID.randomUUID(),
                filename,
                contentType,
                uploadedBy,
                sourcePath,
                sourceType,
                contentHash,
                extractedText,
                now
        ));

        List<DocumentChunkRecord> chunkRecords = new ArrayList<>();
        for (ChunkCandidate chunk : chunks) {
            chunkRecords.add(new DocumentChunkRecord(
                    UUID.randomUUID(),
                    document,
                    chunk.chunkIndex(),
                    chunk.text(),
                    VectorMapper.toFloatArray(embeddingClient.embed(chunk.text())),
                    chunk.pageStart(),
                    chunk.pageEnd(),
                    chunk.paragraphStart(),
                    chunk.paragraphEnd(),
                    now
            ));
        }
        documentChunkRepository.saveAll(chunkRecords);
        return document;
    }

    private String contentTypeFromFilename(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "text/markdown";
        }
        return "text/plain";
    }
}
