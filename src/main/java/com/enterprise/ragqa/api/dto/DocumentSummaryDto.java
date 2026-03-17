package com.enterprise.ragqa.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentSummaryDto(
        UUID documentId,
        String filename,
        String uploadedBy,
        String contentType,
        int chunkCount,
        OffsetDateTime createdAt
) {
}
