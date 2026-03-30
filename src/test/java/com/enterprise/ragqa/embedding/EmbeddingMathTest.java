package com.enterprise.ragqa.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingMathTest {

    @Test
    void identicalVectorsHaveSimilarityOfOne() {
        List<Double> vector = List.of(0.5, 0.3, 0.8, 0.1);
        assertThat(EmbeddingMath.cosineSimilarity(vector, vector)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void orthogonalVectorsHaveSimilarityOfZero() {
        List<Double> a = List.of(1.0, 0.0);
        List<Double> b = List.of(0.0, 1.0);
        assertThat(EmbeddingMath.cosineSimilarity(a, b)).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void oppositeVectorsHaveSimilarityOfMinusOne() {
        List<Double> a = List.of(1.0, 0.0);
        List<Double> b = List.of(-1.0, 0.0);
        assertThat(EmbeddingMath.cosineSimilarity(a, b)).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    void zeroVectorReturnsSimilarityOfZero() {
        List<Double> zero = List.of(0.0, 0.0, 0.0);
        List<Double> nonZero = List.of(1.0, 2.0, 3.0);
        assertThat(EmbeddingMath.cosineSimilarity(zero, nonZero)).isEqualTo(0.0);
        assertThat(EmbeddingMath.cosineSimilarity(nonZero, zero)).isEqualTo(0.0);
    }

    @Test
    void similarityIsSymmetric() {
        List<Double> a = List.of(0.2, 0.5, 0.9);
        List<Double> b = List.of(0.8, 0.1, 0.3);
        double ab = EmbeddingMath.cosineSimilarity(a, b);
        double ba = EmbeddingMath.cosineSimilarity(b, a);
        assertThat(ab).isCloseTo(ba, within(1e-9));
    }

    @Test
    void throwsWhenVectorLengthsDiffer() {
        List<Double> a = List.of(1.0, 2.0);
        List<Double> b = List.of(1.0, 2.0, 3.0);
        assertThatThrownBy(() -> EmbeddingMath.cosineSimilarity(a, b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same length");
    }

    @Test
    void similarityIsWithinValidRange() {
        List<Double> a = List.of(0.3, 0.7, -0.1, 0.9);
        List<Double> b = List.of(0.1, -0.4, 0.6, 0.2);
        double similarity = EmbeddingMath.cosineSimilarity(a, b);
        assertThat(similarity).isBetween(-1.0, 1.0);
    }
}
