package com.enterprise.ragqa.document.service;

import com.enterprise.ragqa.api.dto.DocumentSummaryDto;
import com.enterprise.ragqa.api.dto.DocumentUploadResponse;
import com.enterprise.ragqa.document.model.DocumentChunkRecord;
import com.enterprise.ragqa.document.model.DocumentRecord;
import com.enterprise.ragqa.document.repository.DocumentChunkRepository;
import com.enterprise.ragqa.document.repository.DocumentRepository;
import com.enterprise.ragqa.embedding.EmbeddingClient;
import com.enterprise.ragqa.embedding.EmbeddingSerializer;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionService {

    private final TextExtractionService textExtractionService;
    private final ChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    public DocumentIngestionService(
            TextExtractionService textExtractionService,
            ChunkingService chunkingService,
            EmbeddingClient embeddingClient,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository
    ) {
        this.textExtractionService = textExtractionService;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    @Transactional
    public DocumentUploadResponse ingest(MultipartFile file, String uploadedBy) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        String extractedText = textExtractionService.extract(file);
        List<String> chunks = chunkingService.chunk(extractedText);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("No readable text was extracted from the document.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        DocumentRecord document = documentRepository.save(new DocumentRecord(
                UUID.randomUUID(),
                file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename(),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                uploadedBy,
                extractedText,
                now
        ));

        List<DocumentChunkRecord> chunkRecords = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            chunkRecords.add(new DocumentChunkRecord(
                    UUID.randomUUID(),
                    document,
                    index,
                    chunks.get(index),
                    EmbeddingSerializer.serialize(embeddingClient.embed(chunks.get(index))),
                    now
            ));
        }
        documentChunkRepository.saveAll(chunkRecords);

        return new DocumentUploadResponse(document.getId(), document.getFilename(), chunkRecords.size());
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
}
