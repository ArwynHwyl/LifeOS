package com.example.demo.gamification.dto;

import java.util.List;

public record GamificationRewardDto(
        int expAwarded,
        boolean leveledUp,
        int newLevel,
        List<AchievementDto> achievementsUnlocked
) {
    public static GamificationRewardDto empty() {
        return new GamificationRewardDto(0, false, 0, List.of());
    }
}
