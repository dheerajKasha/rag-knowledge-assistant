package com.enterprise.ragqa.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class HashingEmbeddingClient implements EmbeddingClient {

    private final int dimensions;

    public HashingEmbeddingClient(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public List<Double> embed(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] digestBytes = digest.digest(text.toLowerCase().getBytes(StandardCharsets.UTF_8));
            List<Double> vector = new ArrayList<>(dimensions);
            for (int index = 0; index < dimensions; index++) {
                int bucket = Byte.toUnsignedInt(digestBytes[index % digestBytes.length]);
                vector.add((bucket / 255.0) - 0.5);
            }
            return vector;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable.", exception);
        }
    }
}
