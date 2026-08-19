package com.example.demo.flashcard.dto;

import com.example.demo.gamification.dto.GamificationRewardDto;
import java.time.Instant;

public record SrsReviewResultDto(
        Long srsCardId,
        String outcome,
        Instant nextDueAt,
        GamificationRewardDto reward
) {
}
