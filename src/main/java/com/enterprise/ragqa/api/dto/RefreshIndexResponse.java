package com.enterprise.ragqa.api.dto;

public record RefreshIndexResponse(
        int indexedCount,
        int updatedCount,
        int removedCount,
        String repositoryPath
) {
}
