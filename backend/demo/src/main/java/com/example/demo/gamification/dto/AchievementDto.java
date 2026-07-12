package com.example.demo.gamification.dto;

import java.time.Instant;

public record AchievementDto(
        String code,
        String name,
        String description,
        String iconGlyph,
        int expReward,
        boolean unlocked,
        Instant unlockedAt
) {
}
