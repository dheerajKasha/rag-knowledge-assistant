package com.enterprise.ragqa.config;

import com.enterprise.ragqa.embedding.EmbeddingClient;
import com.enterprise.ragqa.embedding.HashingEmbeddingClient;
import com.enterprise.ragqa.qa.AnswerGenerator;
import com.enterprise.ragqa.qa.ExtractiveAnswerGenerator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiConfiguration {

    @Bean
    EmbeddingClient embeddingClient(AiProviderProperties properties) {
        return new HashingEmbeddingClient(256);
    }

    @Bean
    AnswerGenerator answerGenerator(AiProviderProperties properties) {
        return new ExtractiveAnswerGenerator();
    }
}
