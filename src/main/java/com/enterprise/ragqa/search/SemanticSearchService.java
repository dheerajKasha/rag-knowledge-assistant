package com.enterprise.ragqa.search;

import com.enterprise.ragqa.document.repository.DocumentChunkRepository;
import com.enterprise.ragqa.embedding.EmbeddingClient;
import com.enterprise.ragqa.embedding.EmbeddingMath;
import com.enterprise.ragqa.embedding.EmbeddingSerializer;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SemanticSearchService {

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingClient embeddingClient;

    public SemanticSearchService(DocumentChunkRepository documentChunkRepository, EmbeddingClient embeddingClient) {
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingClient = embeddingClient;
    }

    public List<SearchResult> search(String query, int limit) {
        List<Double> queryEmbedding = embeddingClient.embed(query);
        return documentChunkRepository.findAll().stream()
                .map(chunk -> new SearchResult(
                        chunk,
                        EmbeddingMath.cosineSimilarity(queryEmbedding, EmbeddingSerializer.deserialize(chunk.getEmbeddingJson()))
                ))
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(limit)
                .toList();
    }
}
