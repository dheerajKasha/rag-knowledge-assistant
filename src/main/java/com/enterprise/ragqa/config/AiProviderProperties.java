package com.enterprise.ragqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProviderProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String embeddingModel,
        String chatModel
) {

    public AiProviderProperties {
        provider = provider == null ? "stub" : provider;
        embeddingModel = embeddingModel == null ? "text-embedding-3-small" : embeddingModel;
        chatModel = chatModel == null ? "gpt-4o-mini" : chatModel;
    }
}
