package com.example.demo.flashcard.dto;

import com.example.demo.flashcard.entity.SrsOutcome;
import jakarta.validation.constraints.NotNull;

public record SrsReviewRequest(
        @NotNull SrsOutcome outcome
) {
}
