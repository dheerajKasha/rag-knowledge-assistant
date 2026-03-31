package com.enterprise.ragqa.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AskQuestionRequest(
        @NotBlank String question
) {
}
