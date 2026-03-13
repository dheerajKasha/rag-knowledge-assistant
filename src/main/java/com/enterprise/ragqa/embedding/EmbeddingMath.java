package com.enterprise.ragqa.embedding;

import java.util.List;

public final class EmbeddingMath {

    private EmbeddingMath() {
    }

    public static double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left.size() != right.size()) {
            throw new IllegalArgumentException("Embedding vectors must be the same length.");
        }

        double dotProduct = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int index = 0; index < left.size(); index++) {
            dotProduct += left.get(index) * right.get(index);
            leftNorm += left.get(index) * left.get(index);
            rightNorm += right.get(index) * right.get(index);
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
