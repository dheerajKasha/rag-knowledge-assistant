package com.enterprise.ragqa.embedding;

import java.util.List;

public interface EmbeddingClient {

    List<Double> embed(String text);
}
