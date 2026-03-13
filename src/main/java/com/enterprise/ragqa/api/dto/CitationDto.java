package com.enterprise.ragqa.api.dto;

import java.util.UUID;

public record CitationDto(
        UUID documentId,
        String documentName,
        int chunkIndex,
        String excerpt,
        double score
) {
}
