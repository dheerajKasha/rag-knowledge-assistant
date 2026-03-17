package com.enterprise.ragqa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.documents")
public record DocumentRepositoryProperties(
        String repoPath,
        boolean startupRefreshEnabled
) {

    public DocumentRepositoryProperties {
        repoPath = repoPath == null || repoPath.isBlank() ? "data/documents" : repoPath;
    }
}
