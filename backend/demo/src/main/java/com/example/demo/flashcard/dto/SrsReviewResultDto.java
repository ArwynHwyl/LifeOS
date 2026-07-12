package com.example.demo.flashcard.dto;

import java.time.Instant;

public record SrsReviewResultDto(
        Long srsCardId,
        String outcome,
        Instant nextDueAt
) {
}
