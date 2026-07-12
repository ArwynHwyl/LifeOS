package com.example.demo.gamification.config;

import com.example.demo.gamification.entity.AchievementCriteriaType;
import com.example.demo.gamification.entity.AchievementDefinition;
import com.example.demo.gamification.entity.LevelDefinition;
import com.example.demo.gamification.entity.StreakBonusTier;
import com.example.demo.gamification.repository.AchievementDefinitionRepository;
import com.example.demo.gamification.repository.LevelDefinitionRepository;
import com.example.demo.gamification.repository.StreakBonusTierRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class GamificationSeedConfig {

    private static final int MAX_SEEDED_LEVEL = 30;
    private static final int LEVELS_PER_RANK = 5;
    private static final int LEVELS_PER_SHIELD = 5;

    private static final String[] RANK_NAMES = {
            "Newcomer", "Apprentice", "Scholar", "Strategist", "Virtuoso", "Master"
    };

    @Bean
    CommandLineRunner seedGamificationData(
            LevelDefinitionRepository levelDefinitionRepository,
            StreakBonusTierRepository streakBonusTierRepository,
            AchievementDefinitionRepository achievementDefinitionRepository
    ) {
        return args -> {
            seedLevelDefinitions(levelDefinitionRepository);
            seedStreakBonusTiers(streakBonusTierRepository);
            seedAchievementDefinitions(achievementDefinitionRepository);
        };
    }

    private void seedLevelDefinitions(LevelDefinitionRepository repository) {
        if (repository.count() > 0) {
            return;
        }
        List<LevelDefinition> levels = new ArrayList<>();
        int cumulativeExp = 0;
        for (int level = 1; level <= MAX_SEEDED_LEVEL; level++) {
            if (level > 1) {
                int expToReachFromPrevious = 150 + (level - 2) * 50;
                cumulativeExp += expToReachFromPrevious;
            }
            String rankName = RANK_NAMES[Math.min((level - 1) / LEVELS_PER_RANK, RANK_NAMES.length - 1)];
            int shieldMaxTotal = 1 + (level / LEVELS_PER_SHIELD);
            boolean isRankBandStart = level > 1 && (level - 1) % LEVELS_PER_RANK == 0;
            boolean isShieldMilestone = level % LEVELS_PER_SHIELD == 0;

            String unlockDescription;
            if (isRankBandStart) {
                unlockDescription = "New rank: " + rankName + ".";
            } else if (isShieldMilestone) {
                unlockDescription = "+1 Streak Shield (max now " + shieldMaxTotal + ").";
            } else if (level == 1) {
                unlockDescription = "Welcome! Start earning EXP to level up.";
            } else {
                unlockDescription = "Keep learning to reach level " + (level + 1) + ".";
            }

            levels.add(new LevelDefinition(level, rankName, cumulativeExp, shieldMaxTotal, unlockDescription));
        }
        repository.saveAll(levels);
    }

    private void seedStreakBonusTiers(StreakBonusTierRepository repository) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new StreakBonusTier(3, 5),
                new StreakBonusTier(7, 10),
                new StreakBonusTier(14, 15),
                new StreakBonusTier(30, 20),
                new StreakBonusTier(60, 25)
        ));
    }

    private void seedAchievementDefinitions(AchievementDefinitionRepository repository) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new AchievementDefinition(
                        "FIRST_STEPS", "First Steps", "Complete your first lesson", "★",
                        20, AchievementCriteriaType.FIRST_SUBTOPIC_COMPLETED, 1
                ),
                new AchievementDefinition(
                        "STREAK_7", "7-Day Streak", "Study 7 days in a row", "🔥",
                        50, AchievementCriteriaType.STREAK_DAYS_REACHED, 7
                ),
                new AchievementDefinition(
                        "STREAK_30", "30-Day Streak", "Study 30 days in a row", "🔥",
                        100, AchievementCriteriaType.STREAK_DAYS_REACHED, 30
                ),
                new AchievementDefinition(
                        "QUIZ_ACE", "Quiz Ace", "Perfect score on 5 quizzes", "A⁺",
                        40, AchievementCriteriaType.QUIZ_PERFECT_COUNT, 5
                ),
                new AchievementDefinition(
                        "COURSE_COMPLETE", "Course Complete", "Master every subtopic in a course", "✓",
                        60, AchievementCriteriaType.COURSE_MASTERED_COUNT, 1
                ),
                new AchievementDefinition(
                        "FLASHCARD_MASTER", "Flashcard Master", "Memorize 50 flashcards", "Σ",
                        45, AchievementCriteriaType.FLASHCARDS_MEMORIZED_COUNT, 50
                )
        ));
    }
}
