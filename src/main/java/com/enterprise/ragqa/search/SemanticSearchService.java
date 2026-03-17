package com.enterprise.ragqa.search;

import com.enterprise.ragqa.document.repository.DocumentChunkRepository;
import com.enterprise.ragqa.embedding.EmbeddingClient;
import com.enterprise.ragqa.embedding.VectorMapper;
import java.util.List;
import org.springframework.data.domain.PageRequest;
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
        float[] queryEmbedding = VectorMapper.toFloatArray(embeddingClient.embed(query));
        return documentChunkRepository.findNearestNeighbors(queryEmbedding, PageRequest.of(0, limit)).stream()
                .map(chunk -> new SearchResult(chunk, cosineSimilarity(queryEmbedding, chunk.getEmbedding())))
                .toList();
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left.length != right.length) {
            throw new IllegalArgumentException("Embedding vectors must be the same length.");
        }

        double dotProduct = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int index = 0; index < left.length; index++) {
            dotProduct += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
