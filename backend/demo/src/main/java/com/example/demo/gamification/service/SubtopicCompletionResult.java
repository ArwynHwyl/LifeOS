package com.example.demo.gamification.service;

import com.example.demo.gamification.entity.AchievementDefinition;
import java.util.List;

public record SubtopicCompletionResult(
        int expAwarded,
        boolean leveledUp,
        int newLevel,
        int currentStreak,
        List<AchievementDefinition> unlockedAchievements
) {
}
