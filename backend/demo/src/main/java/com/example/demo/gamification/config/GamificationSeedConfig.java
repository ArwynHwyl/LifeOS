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

    private static final int MAX_SEEDED_LEVEL = 12;
    private static final int LEVELS_PER_RANK = 3;

    private static final String[] RANK_NAMES = {
            "Newcomer", "Scholar", "Strategist", "Master"
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
                int expToReachFromPrevious = 100 + (level - 2) * 40;
                cumulativeExp += expToReachFromPrevious;
            }
            int rankIndex = Math.min((level - 1) / LEVELS_PER_RANK, RANK_NAMES.length - 1);
            String rankName = RANK_NAMES[rankIndex];
            int shieldMaxTotal = rankIndex + 1;
            boolean isRankBandStart = level > 1 && (level - 1) % LEVELS_PER_RANK == 0;

            String unlockDescription;
            if (level == 1) {
                unlockDescription = "Welcome! Start earning EXP to level up.";
            } else if (isRankBandStart) {
                unlockDescription = "New rank: " + rankName + ". Shield capacity now " + shieldMaxTotal + ".";
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
                        "ASSESSMENT_ROOKIE", "Assessment Rookie", "Score a perfect result on your first assessment", "B",
                        15, AchievementCriteriaType.ASSESSMENT_PERFECT_COUNT, 1
                ),
                new AchievementDefinition(
                        "FLASHCARD_STARTER", "Flashcard Starter", "Memorize your first flashcard", "σ",
                        15, AchievementCriteriaType.FLASHCARDS_MEMORIZED_COUNT, 1
                ),
                new AchievementDefinition(
                        "STREAK_STARTER", "Streak Starter", "Study 3 days in a row", "🔥",
                        25, AchievementCriteriaType.STREAK_DAYS_REACHED, 3
                ),
                new AchievementDefinition(
                        "STREAK_7", "7-Day Streak", "Study 7 days in a row", "🔥",
                        50, AchievementCriteriaType.STREAK_DAYS_REACHED, 7
                ),
                new AchievementDefinition(
                        "RISING_STAR", "Rising Star", "Reach level 7 and become a Strategist", "🌟",
                        60, AchievementCriteriaType.LEVEL_REACHED, 7
                ),
                new AchievementDefinition(
                        "ASSESSMENT_ACE", "Assessment Ace", "Perfect score on 5 assessments", "A⁺",
                        40, AchievementCriteriaType.ASSESSMENT_PERFECT_COUNT, 5
                ),
                new AchievementDefinition(
                        "COURSE_COMPLETE", "Course Complete", "Master every subtopic in a course", "✓",
                        60, AchievementCriteriaType.COURSE_MASTERED_COUNT, 1
                ),
                new AchievementDefinition(
                        "GRANDMASTER", "Grandmaster", "Reach level 12, the top of the roadmap", "♛",
                        100, AchievementCriteriaType.LEVEL_REACHED, 12
                )
        ));
    }
}
