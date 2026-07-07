package com.example.demo.gamification.dto;

public record LevelRoadmapEntryDto(
        int level,
        String rankName,
        int expRequiredToReach,
        int shieldMaxTotal,
        String unlockDescription,
        boolean achieved
) {
}
