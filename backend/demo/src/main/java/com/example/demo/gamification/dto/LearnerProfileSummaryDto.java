package com.example.demo.gamification.dto;

import java.time.Instant;

public record LearnerProfileSummaryDto(
        int level,
        String rankName,
        int currentExp,
        int expRequiredForNextLevel,
        long totalExp,
        int currentStreak,
        int longestStreak,
        int streakBonusPercent,
        int currentShield,
        int shieldMax,
        Instant memberSince
) {
}
