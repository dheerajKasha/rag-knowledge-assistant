package com.enterprise.ragqa.embedding;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class EmbeddingSerializer {

    private EmbeddingSerializer() {
    }

    public static String serialize(List<Double> embedding) {
        return embedding.stream()
                .map(value -> String.format(Locale.US, "%.8f", value))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    public static List<Double> deserialize(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return List.of();
        }
        return Arrays.stream(serialized.split(","))
                .map(Double::parseDouble)
                .toList();
    }
}
