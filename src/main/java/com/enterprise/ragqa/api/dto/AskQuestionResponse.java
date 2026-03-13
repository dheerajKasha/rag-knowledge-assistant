package com.enterprise.ragqa.api.dto;

import java.util.List;
import java.util.UUID;

public record AskQuestionResponse(
        UUID queryId,
        String answer,
        List<CitationDto> citations
) {
}
