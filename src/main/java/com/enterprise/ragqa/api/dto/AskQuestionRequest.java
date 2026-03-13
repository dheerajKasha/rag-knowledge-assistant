package com.enterprise.ragqa.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AskQuestionRequest(
        @NotBlank String question,
        String userId,
        @Min(1) @Max(10) Integer maxResults
) {

    public int resolvedMaxResults() {
        return maxResults == null ? 5 : maxResults;
    }

    public String resolvedUserId() {
        return userId == null || userId.isBlank() ? "anonymous" : userId;
    }
}
