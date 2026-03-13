package com.enterprise.ragqa.api.dto;

import java.util.UUID;

public record DocumentUploadResponse(
        UUID documentId,
        String filename,
        int chunksIndexed
) {
}
