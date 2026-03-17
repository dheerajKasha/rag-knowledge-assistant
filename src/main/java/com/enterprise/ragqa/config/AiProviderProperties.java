package com.enterprise.ragqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProviderProperties(
        String provider,
        String embeddingBaseUrl,
        String chatBaseUrl,
        String apiKey,
        String embeddingModel,
        String chatModel
) {

    public AiProviderProperties {
        provider = provider == null ? "stub" : provider;
        embeddingBaseUrl = embeddingBaseUrl == null ? "http://localhost:11434/v1" : embeddingBaseUrl;
        chatBaseUrl = chatBaseUrl == null ? "http://localhost:11434/v1" : chatBaseUrl;
        embeddingModel = embeddingModel == null ? "text-embedding-3-small" : embeddingModel;
        chatModel = chatModel == null ? "gpt-4o-mini" : chatModel;
    }
}
