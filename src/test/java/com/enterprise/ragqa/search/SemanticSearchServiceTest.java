package com.enterprise.ragqa.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enterprise.ragqa.document.model.DocumentChunkRecord;
import com.enterprise.ragqa.document.model.DocumentRecord;
import com.enterprise.ragqa.document.repository.DocumentChunkRepository;
import com.enterprise.ragqa.document.service.DocumentSourceType;
import com.enterprise.ragqa.embedding.EmbeddingClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private EmbeddingClient embeddingClient;

    @InjectMocks
    private SemanticSearchService semanticSearchService;

    private DocumentRecord document;
    private DocumentChunkRecord chunk;

    @BeforeEach
    void setUp() {
        document = new DocumentRecord(
                UUID.randomUUID(), "test.pdf", "application/pdf", "tester",
                null, DocumentSourceType.UPLOAD, "hash", "Some text", OffsetDateTime.now()
        );
        chunk = new DocumentChunkRecord(
                UUID.randomUUID(), document, 0, "Relevant content about benefits.",
                new float[]{0.5f, 0.5f}, 1, 1, 1, 1, OffsetDateTime.now()
        );
    }

    @Test
    void returnsSearchResultsWithScores() {
        when(embeddingClient.embed("benefits query")).thenReturn(List.of(0.5, 0.5));
        when(documentChunkRepository.findNearestNeighbors(any(float[].class), eq(PageRequest.of(0, 3))))
                .thenReturn(List.of(chunk));

        List<SearchResult> results = semanticSearchService.search("benefits query", 3);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunk()).isEqualTo(chunk);
        assertThat(results.get(0).score()).isBetween(-1.0, 1.0);
    }

    @Test
    void returnsEmptyListWhenNoChunksFound() {
        when(embeddingClient.embed("unknown topic")).thenReturn(List.of(0.1, 0.9));
        when(documentChunkRepository.findNearestNeighbors(any(float[].class), any()))
                .thenReturn(List.of());

        List<SearchResult> results = semanticSearchService.search("unknown topic", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void embedsQueryBeforeSearching() {
        when(embeddingClient.embed("my query")).thenReturn(List.of(0.3, 0.7));
        when(documentChunkRepository.findNearestNeighbors(any(float[].class), any()))
                .thenReturn(List.of());

        semanticSearchService.search("my query", 5);

        verify(embeddingClient).embed("my query");
        verify(documentChunkRepository).findNearestNeighbors(any(float[].class), eq(PageRequest.of(0, 5)));
    }

    @Test
    void scoreIsHighForIdenticalEmbeddings() {
        float[] embedding = {0.6f, 0.8f};
        DocumentChunkRecord matchingChunk = new DocumentChunkRecord(
                UUID.randomUUID(), document, 1, "Identical embedding chunk.",
                embedding, 2, 2, 3, 3, OffsetDateTime.now()
        );
        when(embeddingClient.embed("query")).thenReturn(List.of(0.6, 0.8));
        when(documentChunkRepository.findNearestNeighbors(any(float[].class), any()))
                .thenReturn(List.of(matchingChunk));

        List<SearchResult> results = semanticSearchService.search("query", 1);

        assertThat(results.get(0).score()).isGreaterThan(0.99);
    }
}
