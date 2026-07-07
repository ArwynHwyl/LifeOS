package com.example.demo.gamification.dto;

import java.time.Instant;

public record PendingLearnerEventDto(
        Long id,
        String eventType,
        Integer shieldsRemaining,
        Integer shieldMax,
        Integer streakDaysLost,
        Integer longestStreak,
        Instant createdAt
) {
}
