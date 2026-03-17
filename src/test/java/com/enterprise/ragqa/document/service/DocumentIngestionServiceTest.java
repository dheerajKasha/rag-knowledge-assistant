package com.enterprise.ragqa.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.enterprise.ragqa.document.model.DocumentRecord;
import com.enterprise.ragqa.document.repository.DocumentChunkRepository;
import com.enterprise.ragqa.document.repository.DocumentRepository;
import com.enterprise.ragqa.embedding.EmbeddingClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private TextExtractionService textExtractionService;

    @Mock
    private ChunkingService chunkingService;

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DocumentHashingService documentHashingService;

    @Test
    void listsDocumentsWithChunkCounts() {
        DocumentRecord first = new DocumentRecord(
                UUID.randomUUID(),
                "policy.pdf",
                "application/pdf",
                "ops-user",
                "repo/policy.pdf",
                DocumentSourceType.REPOSITORY,
                "hash-1",
                "Policy content",
                OffsetDateTime.now()
        );
        DocumentRecord second = new DocumentRecord(
                UUID.randomUUID(),
                "handbook.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "hr-user",
                null,
                DocumentSourceType.UPLOAD,
                "hash-2",
                "Handbook content",
                OffsetDateTime.now().minusDays(1)
        );

        when(documentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))).thenReturn(List.of(first, second));
        when(documentChunkRepository.countByDocument_Id(first.getId())).thenReturn(4L);
        when(documentChunkRepository.countByDocument_Id(second.getId())).thenReturn(7L);

        DocumentIngestionService service = new DocumentIngestionService(
                textExtractionService,
                chunkingService,
                embeddingClient,
                documentRepository,
                documentChunkRepository,
                documentHashingService
        );

        var documents = service.listDocuments();

        assertThat(documents).hasSize(2);
        assertThat(documents.get(0).filename()).isEqualTo("policy.pdf");
        assertThat(documents.get(0).chunkCount()).isEqualTo(4);
        assertThat(documents.get(1).filename()).isEqualTo("handbook.docx");
        assertThat(documents.get(1).chunkCount()).isEqualTo(7);
    }
}
