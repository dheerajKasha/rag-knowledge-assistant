package com.enterprise.ragqa.api.dto;

import java.util.UUID;

public record CitationDto(
        UUID documentId,
        String documentName,
        int chunkIndex,
        int pageStart,
        int pageEnd,
        int paragraphStart,
        int paragraphEnd,
        String excerpt,
        double score
) {
}
