package com.example.demo.gamification.service;

import com.example.demo.gamification.dto.AchievementDto;
import com.example.demo.gamification.dto.DailyActivityDto;
import com.example.demo.gamification.dto.LearnerProfileSummaryDto;
import com.example.demo.gamification.dto.LevelRoadmapEntryDto;
import com.example.demo.gamification.dto.PendingLearnerEventDto;
import com.example.demo.gamification.entity.AchievementCriteriaType;
import com.example.demo.gamification.entity.AchievementDefinition;
import com.example.demo.gamification.entity.DailyLearningActivity;
import com.example.demo.gamification.entity.LearnerEvent;
import com.example.demo.gamification.entity.LearnerProfile;
import com.example.demo.gamification.entity.LevelDefinition;
import com.example.demo.gamification.entity.StreakBonusTier;
import com.example.demo.gamification.entity.UserAchievement;
import com.example.demo.gamification.repository.AchievementDefinitionRepository;
import com.example.demo.gamification.repository.DailyLearningActivityRepository;
import com.example.demo.gamification.repository.LearnerEventRepository;
import com.example.demo.gamification.repository.LearnerProfileRepository;
import com.example.demo.gamification.repository.LevelDefinitionRepository;
import com.example.demo.gamification.repository.StreakBonusTierRepository;
import com.example.demo.gamification.repository.UserAchievementRepository;
import com.example.demo.shared.exception.AccessDeniedException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamificationService {

    private final LearnerProfileRepository learnerProfileRepository;
    private final LevelDefinitionRepository levelDefinitionRepository;
    private final StreakBonusTierRepository streakBonusTierRepository;
    private final AchievementDefinitionRepository achievementDefinitionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final DailyLearningActivityRepository dailyLearningActivityRepository;
    private final LearnerEventRepository learnerEventRepository;
    private final UserRepository userRepository;

    public GamificationService(
            LearnerProfileRepository learnerProfileRepository,
            LevelDefinitionRepository levelDefinitionRepository,
            StreakBonusTierRepository streakBonusTierRepository,
            AchievementDefinitionRepository achievementDefinitionRepository,
            UserAchievementRepository userAchievementRepository,
            DailyLearningActivityRepository dailyLearningActivityRepository,
            LearnerEventRepository learnerEventRepository,
            UserRepository userRepository
    ) {
        this.learnerProfileRepository = learnerProfileRepository;
        this.levelDefinitionRepository = levelDefinitionRepository;
        this.streakBonusTierRepository = streakBonusTierRepository;
        this.achievementDefinitionRepository = achievementDefinitionRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.dailyLearningActivityRepository = dailyLearningActivityRepository;
        this.learnerEventRepository = learnerEventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LearnerProfile getOrCreateProfile(UUID userId) {
        return learnerProfileRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    LearnerProfile profile = new LearnerProfile(user);
                    LevelDefinition levelOne = levelDefinitionRepository.findById(1)
                            .orElseThrow(() -> new IllegalStateException("Level 1 definition is missing"));
                    profile.setCurrentShield(levelOne.getShieldMaxTotal());
                    profile.setShieldPoolLastResetAt(Instant.now());
                    return learnerProfileRepository.save(profile);
                });
    }

    public int getActiveStreakBonusPercent(int currentStreak) {
        int bonusPercent = 0;
        for (StreakBonusTier tier : streakBonusTierRepository.findAllByOrderByMinStreakDaysAsc()) {
            if (currentStreak >= tier.getMinStreakDays()) {
                bonusPercent = tier.getBonusPercent();
            }
        }
        return bonusPercent;
    }

    /**
     * Entry point for the future subtopic-mastery hook. Not called anywhere yet —
     * wiring it into LearnerCourseService requires a separate confirmed change.
     */
    @Transactional
    public SubtopicCompletionResult recordSubtopicCompletion(UUID userId, int baseExp) {
        LearnerProfile profile = getOrCreateProfile(userId);
        LocalDate today = LocalDate.now();

        boolean isFirstSubtopicToday = updateDailyActivityAndStreak(profile, today, baseExp);

        int bonusPercent = getActiveStreakBonusPercent(profile.getCurrentStreak());
        int expAwarded = baseExp + (baseExp * bonusPercent / 100);

        LevelUpOutcome levelUpOutcome = applyExpGain(profile, expAwarded);

        List<AchievementDefinition> unlocked = new ArrayList<>();
        if (isFirstSubtopicToday) {
            unlocked.addAll(unlockEligibleAchievements(profile, AchievementCriteriaType.FIRST_SUBTOPIC_COMPLETED, 1));
            unlocked.addAll(unlockEligibleAchievements(
                    profile, AchievementCriteriaType.STREAK_DAYS_REACHED, profile.getCurrentStreak()));
        }

        learnerProfileRepository.save(profile);
        return new SubtopicCompletionResult(
                expAwarded, levelUpOutcome.leveledUp(), levelUpOutcome.newLevel(), profile.getCurrentStreak(), unlocked);
    }

    public List<AchievementDefinition> notifyQuizPerfect(UUID userId, int totalPerfectQuizzes) {
        LearnerProfile profile = getOrCreateProfile(userId);
        List<AchievementDefinition> unlocked = unlockEligibleAchievements(
                profile, AchievementCriteriaType.QUIZ_PERFECT_COUNT, totalPerfectQuizzes);
        learnerProfileRepository.save(profile);
        return unlocked;
    }

    public List<AchievementDefinition> notifyCourseMastered(UUID userId, int totalCoursesMastered) {
        LearnerProfile profile = getOrCreateProfile(userId);
        List<AchievementDefinition> unlocked = unlockEligibleAchievements(
                profile, AchievementCriteriaType.COURSE_MASTERED_COUNT, totalCoursesMastered);
        learnerProfileRepository.save(profile);
        return unlocked;
    }

    public List<AchievementDefinition> notifyFlashcardsMemorized(UUID userId, int totalMemorized) {
        LearnerProfile profile = getOrCreateProfile(userId);
        List<AchievementDefinition> unlocked = unlockEligibleAchievements(
                profile, AchievementCriteriaType.FLASHCARDS_MEMORIZED_COUNT, totalMemorized);
        learnerProfileRepository.save(profile);
        return unlocked;
    }

    private boolean updateDailyActivityAndStreak(LearnerProfile profile, LocalDate today, int expEarnedToday) {
        DailyLearningActivity activity = dailyLearningActivityRepository
                .findByUserUserIdAndActivityDate(profile.getUserId(), today)
                .orElseGet(() -> new DailyLearningActivity(profile.getUser(), today));
        boolean isFirstSubtopicToday = activity.getSubtopicsCompleted() == 0;
        activity.recordSubtopicCompletion(expEarnedToday);
        dailyLearningActivityRepository.save(activity);

        if (isFirstSubtopicToday) {
            updateStreakForNewActivity(profile, today);
        }
        return isFirstSubtopicToday;
    }

    private void updateStreakForNewActivity(LearnerProfile profile, LocalDate today) {
        LocalDate effectiveLastSafeDate = latestOf(profile.getLastActiveDate(), profile.getStreakSafeThroughDate());
        if (effectiveLastSafeDate == null || !effectiveLastSafeDate.equals(today.minusDays(1))) {
            if (effectiveLastSafeDate == null || !effectiveLastSafeDate.equals(today)) {
                profile.setCurrentStreak(1);
            }
        } else {
            profile.setCurrentStreak(profile.getCurrentStreak() + 1);
        }
        profile.setLastActiveDate(today);
        profile.setStreakSafeThroughDate(null);
    }

    private LevelUpOutcome applyExpGain(LearnerProfile profile, int expAmount) {
        if (expAmount <= 0) {
            return new LevelUpOutcome(false, profile.getLevel());
        }
        int previousLevel = profile.getLevel();
        profile.addTotalExp(expAmount);

        LevelDefinition achievedLevel = null;
        for (LevelDefinition levelDefinition : levelDefinitionRepository.findAllByOrderByLevelAsc()) {
            if (levelDefinition.getExpRequiredToReach() <= profile.getTotalExp()) {
                achievedLevel = levelDefinition;
            } else {
                break;
            }
        }
        if (achievedLevel == null) {
            throw new IllegalStateException("No level definitions seeded");
        }

        profile.setLevel(achievedLevel.getLevel());
        profile.setCurrentExp((int) (profile.getTotalExp() - achievedLevel.getExpRequiredToReach()));
        return new LevelUpOutcome(achievedLevel.getLevel() > previousLevel, achievedLevel.getLevel());
    }

    private List<AchievementDefinition> unlockEligibleAchievements(
            LearnerProfile profile, AchievementCriteriaType criteriaType, int currentValue) {
        List<AchievementDefinition> newlyUnlocked = new ArrayList<>();
        for (AchievementDefinition achievement : achievementDefinitionRepository.findAll()) {
            if (achievement.getCriteriaType() != criteriaType) {
                continue;
            }
            if (currentValue < achievement.getCriteriaValue()) {
                continue;
            }
            if (userAchievementRepository.existsByUserUserIdAndAchievementId(profile.getUserId(), achievement.getId())) {
                continue;
            }
            userAchievementRepository.save(new UserAchievement(profile.getUser(), achievement));
            applyExpGain(profile, achievement.getExpReward());
            newlyUnlocked.add(achievement);
        }
        return newlyUnlocked;
    }

    @Transactional
    public void runDailyStreakMaintenance() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        for (LearnerProfile profile : learnerProfileRepository.findAll()) {
            if (profile.getCurrentStreak() <= 0) {
                continue;
            }
            LocalDate effectiveLastSafeDate = latestOf(profile.getLastActiveDate(), profile.getStreakSafeThroughDate());
            if (effectiveLastSafeDate != null && !effectiveLastSafeDate.isBefore(yesterday)) {
                continue;
            }
            if (profile.getCurrentShield() > 0) {
                profile.setCurrentShield(profile.getCurrentShield() - 1);
                profile.setStreakSafeThroughDate(yesterday);
                learnerEventRepository.save(LearnerEvent.shieldConsumed(
                        profile.getUser(), profile.getCurrentShield(), currentShieldMax(profile)));
            } else {
                int daysLost = profile.getCurrentStreak();
                int longestStreak = profile.getLongestStreak();
                profile.setCurrentStreak(0);
                profile.setStreakSafeThroughDate(null);
                learnerEventRepository.save(LearnerEvent.streakLost(profile.getUser(), daysLost, longestStreak));
            }
            learnerProfileRepository.save(profile);
        }
    }

    @Transactional
    public void runWeeklyShieldRefill() {
        Instant now = Instant.now();
        for (LearnerProfile profile : learnerProfileRepository.findAll()) {
            int shieldMax = currentShieldMax(profile);
            if (profile.getCurrentShield() < shieldMax) {
                profile.setCurrentShield(shieldMax);
            }
            profile.setShieldPoolLastResetAt(now);
            learnerProfileRepository.save(profile);
        }
    }

    private int currentShieldMax(LearnerProfile profile) {
        return levelDefinitionRepository.findById(profile.getLevel())
                .map(LevelDefinition::getShieldMaxTotal)
                .orElse(profile.getCurrentShield());
    }

    private static LocalDate latestOf(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    public List<LearnerEvent> getPendingEvents(UUID userId) {
        return learnerEventRepository.findByUserUserIdAndAcknowledgedAtIsNullOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public void acknowledgeEvent(UUID userId, Long eventId) {
        LearnerEvent event = learnerEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Learner event not found: " + eventId));
        if (!event.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Learner event " + eventId + " does not belong to user " + userId);
        }
        event.acknowledge();
        learnerEventRepository.save(event);
    }

    public List<LevelDefinition> getLevelRoadmap() {
        return levelDefinitionRepository.findAllByOrderByLevelAsc();
    }

    public List<AchievementDefinition> getAchievementCatalog() {
        return achievementDefinitionRepository.findAll();
    }

    public List<UserAchievement> getUnlockedAchievements(UUID userId) {
        return userAchievementRepository.findByUserUserId(userId);
    }

    public LearnerProfileSummaryDto getProfileSummary(UUID userId) {
        LearnerProfile profile = getOrCreateProfile(userId);
        List<LevelDefinition> levels = levelDefinitionRepository.findAllByOrderByLevelAsc();
        LevelDefinition currentLevel = levels.stream()
                .filter(level -> level.getLevel() == profile.getLevel())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing level definition for level " + profile.getLevel()));
        int expRequiredForNextLevel = levels.stream()
                .filter(level -> level.getLevel() == profile.getLevel() + 1)
                .findFirst()
                .map(next -> next.getExpRequiredToReach() - currentLevel.getExpRequiredToReach())
                .orElse(-1);

        return new LearnerProfileSummaryDto(
                profile.getLevel(),
                currentLevel.getRankName(),
                profile.getCurrentExp(),
                expRequiredForNextLevel,
                profile.getTotalExp(),
                profile.getCurrentStreak(),
                profile.getLongestStreak(),
                getActiveStreakBonusPercent(profile.getCurrentStreak()),
                profile.getCurrentShield(),
                currentLevel.getShieldMaxTotal(),
                profile.getUser().getCreateAt()
        );
    }

    public List<LevelRoadmapEntryDto> getLevelRoadmapDto(UUID userId) {
        LearnerProfile profile = getOrCreateProfile(userId);
        return getLevelRoadmap().stream()
                .map(level -> new LevelRoadmapEntryDto(
                        level.getLevel(),
                        level.getRankName(),
                        level.getExpRequiredToReach(),
                        level.getShieldMaxTotal(),
                        level.getUnlockDescription(),
                        level.getLevel() <= profile.getLevel()
                ))
                .toList();
    }

    public List<AchievementDto> getAchievementCatalogDto(UUID userId) {
        Map<Long, Instant> unlockedAtById = getUnlockedAchievements(userId).stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), UserAchievement::getUnlockedAt));

        return getAchievementCatalog().stream()
                .map(achievement -> new AchievementDto(
                        achievement.getCode(),
                        achievement.getName(),
                        achievement.getDescription(),
                        achievement.getIconGlyph(),
                        achievement.getExpReward(),
                        unlockedAtById.containsKey(achievement.getId()),
                        unlockedAtById.get(achievement.getId())
                ))
                .toList();
    }

    public List<PendingLearnerEventDto> getPendingEventDtos(UUID userId) {
        return getPendingEvents(userId).stream()
                .map(event -> new PendingLearnerEventDto(
                        event.getId(),
                        event.getEventType().name(),
                        event.getShieldsRemaining(),
                        event.getShieldMax(),
                        event.getStreakDaysLost(),
                        event.getLongestStreak(),
                        event.getCreatedAt()
                ))
                .toList();
    }

    public List<DailyActivityDto> getRecentActivityDto(UUID userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);
        Map<LocalDate, DailyLearningActivity> byDate = dailyLearningActivityRepository
                .findByUserUserIdAndActivityDateBetweenOrderByActivityDateAsc(userId, start, today)
                .stream()
                .collect(Collectors.toMap(DailyLearningActivity::getActivityDate, activity -> activity));

        List<DailyActivityDto> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            DailyLearningActivity activity = byDate.get(date);
            int completed = activity != null ? activity.getSubtopicsCompleted() : 0;
            int expEarned = activity != null ? activity.getExpEarned() : 0;
            result.add(new DailyActivityDto(date, completed, expEarned));
        }
        return result;
    }
}
