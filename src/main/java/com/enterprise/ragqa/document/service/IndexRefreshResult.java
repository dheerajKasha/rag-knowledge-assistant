package com.enterprise.ragqa.document.service;

public record IndexRefreshResult(
        int indexedCount,
        int updatedCount,
        int removedCount
) {
}
