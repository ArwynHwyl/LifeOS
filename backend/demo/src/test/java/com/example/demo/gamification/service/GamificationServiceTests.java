package com.example.demo.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.gamification.dto.AchievementDto;
import com.example.demo.gamification.dto.LearnerProfileSummaryDto;
import com.example.demo.gamification.dto.LevelRoadmapEntryDto;
import com.example.demo.gamification.dto.PendingLearnerEventDto;
import com.example.demo.gamification.entity.AchievementCriteriaType;
import com.example.demo.gamification.entity.AchievementDefinition;
import com.example.demo.gamification.entity.DailyLearningActivity;
import com.example.demo.gamification.entity.LearnerEvent;
import com.example.demo.gamification.entity.LearnerEventType;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTests {

    @Mock
    private LearnerProfileRepository learnerProfileRepository;

    @Mock
    private LevelDefinitionRepository levelDefinitionRepository;

    @Mock
    private StreakBonusTierRepository streakBonusTierRepository;

    @Mock
    private AchievementDefinitionRepository achievementDefinitionRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private DailyLearningActivityRepository dailyLearningActivityRepository;

    @Mock
    private LearnerEventRepository learnerEventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GamificationService service;

    private LearnerProfile spyProfile(UUID userId) {
        User user = mock(User.class);
        LearnerProfile profile = spy(new LearnerProfile(user));
        lenient().doReturn(userId).when(profile).getUserId();
        return profile;
    }

    // UTC-19_TC-01
    @Test
    void getProfileSummaryComputesExpRequiredForNextLevel() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = spyProfile(userId);
        profile.setLevel(3);
        profile.setCurrentExp(20);
        profile.addTotalExp(160);
        profile.setCurrentStreak(5);
        profile.setCurrentShield(2);
        Instant memberSince = Instant.parse("2026-01-10T00:00:00Z");
        when(profile.getUser().getCreateAt()).thenReturn(memberSince);

        when(learnerProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(levelDefinitionRepository.findAllByOrderByLevelAsc()).thenReturn(List.of(
                new LevelDefinition(1, "Newcomer", 0, 1, "Start"),
                new LevelDefinition(2, "Newcomer", 100, 1, "Keep going"),
                new LevelDefinition(3, "Newcomer", 140, 1, "Keep going"),
                new LevelDefinition(4, "Newcomer", 180, 1, "Keep going")
        ));
        when(streakBonusTierRepository.findAllByOrderByMinStreakDaysAsc())
                .thenReturn(List.of(new StreakBonusTier(3, 5)));

        LearnerProfileSummaryDto summary = service.getProfileSummary(userId);

        assertThat(summary.level()).isEqualTo(3);
        assertThat(summary.rankName()).isEqualTo("Newcomer");
        assertThat(summary.currentExp()).isEqualTo(20);
        assertThat(summary.expRequiredForNextLevel()).isEqualTo(40);
        assertThat(summary.totalExp()).isEqualTo(160);
        assertThat(summary.streakBonusPercent()).isEqualTo(5);
        assertThat(summary.shieldMax()).isEqualTo(1);
        assertThat(summary.memberSince()).isEqualTo(memberSince);
    }

    // UTC-19_TC-02
    @Test
    void getProfileSummaryReturnsMinusOneAtMaxSeededLevel() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = spyProfile(userId);
        profile.setLevel(12);
        when(profile.getUser().getCreateAt()).thenReturn(Instant.now());

        when(learnerProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(levelDefinitionRepository.findAllByOrderByLevelAsc()).thenReturn(List.of(
                new LevelDefinition(11, "Master", 900, 4, "Keep going"),
                new LevelDefinition(12, "Master", 1000, 4, "Top of the roadmap")
        ));
        when(streakBonusTierRepository.findAllByOrderByMinStreakDaysAsc()).thenReturn(List.of());

        LearnerProfileSummaryDto summary = service.getProfileSummary(userId);

        assertThat(summary.expRequiredForNextLevel()).isEqualTo(-1);
    }

    // UTC-19_TC-03
    @Test
    void getLevelRoadmapDtoMarksAchievedLevelsUpToProfileLevel() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = spyProfile(userId);
        profile.setLevel(2);

        when(learnerProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(levelDefinitionRepository.findAllByOrderByLevelAsc()).thenReturn(List.of(
                new LevelDefinition(1, "Newcomer", 0, 1, "Start"),
                new LevelDefinition(2, "Newcomer", 100, 1, "Keep going"),
                new LevelDefinition(3, "Scholar", 140, 2, "New rank")
        ));

        List<LevelRoadmapEntryDto> roadmap = service.getLevelRoadmapDto(userId);

        assertThat(roadmap).extracting(LevelRoadmapEntryDto::level).containsExactly(1, 2, 3);
        assertThat(roadmap).filteredOn(entry -> entry.level() <= 2).allMatch(LevelRoadmapEntryDto::achieved);
        assertThat(roadmap.get(2).achieved()).isFalse();
    }

    // UTC-19_TC-04
    @Test
    void getAchievementCatalogDtoMergesUnlockedAchievements() {
        UUID userId = UUID.randomUUID();
        AchievementDefinition unlockedDef = mock(AchievementDefinition.class);
        when(unlockedDef.getId()).thenReturn(1L);
        when(unlockedDef.getCode()).thenReturn("FIRST_STEPS");
        AchievementDefinition lockedDef = mock(AchievementDefinition.class);
        when(lockedDef.getId()).thenReturn(2L);
        when(lockedDef.getCode()).thenReturn("STREAK_STARTER");

        UserAchievement unlocked = new UserAchievement(mock(User.class), unlockedDef);

        when(achievementDefinitionRepository.findAll()).thenReturn(List.of(unlockedDef, lockedDef));
        when(userAchievementRepository.findByUserUserId(userId)).thenReturn(List.of(unlocked));

        List<AchievementDto> catalog = service.getAchievementCatalogDto(userId);

        AchievementDto unlockedDto = catalog.stream().filter(a -> a.code().equals("FIRST_STEPS")).findFirst().orElseThrow();
        AchievementDto lockedDto = catalog.stream().filter(a -> a.code().equals("STREAK_STARTER")).findFirst().orElseThrow();
        assertThat(unlockedDto.unlocked()).isTrue();
        assertThat(unlockedDto.unlockedAt()).isNotNull();
        assertThat(lockedDto.unlocked()).isFalse();
        assertThat(lockedDto.unlockedAt()).isNull();
    }

    // UTC-20_TC-01
    @Test
    void getPendingEventDtosReturnsQueueOrderedByCreatedAt() {
        UUID userId = UUID.randomUUID();
        LearnerEvent event = mock(LearnerEvent.class);
        when(event.getId()).thenReturn(9L);
        when(event.getEventType()).thenReturn(LearnerEventType.SHIELD_CONSUMED);
        when(event.getShieldsRemaining()).thenReturn(1);
        when(event.getShieldMax()).thenReturn(2);
        when(event.getCreatedAt()).thenReturn(Instant.parse("2026-02-01T00:10:00Z"));

        when(learnerEventRepository.findByUserUserIdAndAcknowledgedAtIsNullOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(event));

        List<PendingLearnerEventDto> pending = service.getPendingEventDtos(userId);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).eventType()).isEqualTo("SHIELD_CONSUMED");
        assertThat(pending.get(0).shieldsRemaining()).isEqualTo(1);
    }

    // UTC-20_TC-02
    @Test
    void acknowledgeEventMarksOwnedEventAcknowledged() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        LearnerEvent event = mock(LearnerEvent.class);
        when(event.getUser()).thenReturn(user);

        when(learnerEventRepository.findById(5L)).thenReturn(Optional.of(event));

        service.acknowledgeEvent(userId, 5L);

        verify(event).acknowledge();
        verify(learnerEventRepository).save(event);
    }

    // UTC-20_TC-03
    @Test
    void acknowledgeEventForOtherUserThrowsAccessDeniedException() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        User owner = mock(User.class);
        when(owner.getUserId()).thenReturn(otherUserId);
        LearnerEvent event = mock(LearnerEvent.class);
        when(event.getUser()).thenReturn(owner);

        when(learnerEventRepository.findById(5L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.acknowledgeEvent(userId, 5L))
                .isInstanceOf(AccessDeniedException.class);
        verify(event, never()).acknowledge();
    }

    // UTC-20_TC-04
    @Test
    void acknowledgeEventUnknownIdThrowsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(learnerEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acknowledgeEvent(userId, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Learner event not found: 999");
    }

    // UTC-21_TC-01
    @Test
    void getActiveStreakBonusPercentReturnsHighestQualifyingTier() {
        when(streakBonusTierRepository.findAllByOrderByMinStreakDaysAsc()).thenReturn(List.of(
                new StreakBonusTier(3, 5),
                new StreakBonusTier(7, 10),
                new StreakBonusTier(14, 15)
        ));

        assertThat(service.getActiveStreakBonusPercent(10)).isEqualTo(10);
        assertThat(service.getActiveStreakBonusPercent(20)).isEqualTo(15);
    }

    // UTC-21_TC-02
    @Test
    void getActiveStreakBonusPercentReturnsZeroBelowLowestTier() {
        when(streakBonusTierRepository.findAllByOrderByMinStreakDaysAsc())
                .thenReturn(List.of(new StreakBonusTier(3, 5)));

        assertThat(service.getActiveStreakBonusPercent(2)).isEqualTo(0);
    }

    // UTC-21_TC-03
    @Test
    void recordSubtopicCompletionContinuesStreakAndAppliesBonusAndLevelUp() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = spyProfile(userId);
        profile.setLevel(1);
        profile.setCurrentStreak(2);
        profile.setLastActiveDate(LocalDate.now().minusDays(1));

        when(learnerProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(dailyLearningActivityRepository.findByUserUserIdAndActivityDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(streakBonusTierRepository.findAllByOrderByMinStreakDaysAsc())
                .thenReturn(List.of(new StreakBonusTier(3, 5)));
        when(levelDefinitionRepository.findAllByOrderByLevelAsc()).thenReturn(List.of(
                new LevelDefinition(1, "Newcomer", 0, 1, "Start"),
                new LevelDefinition(2, "Newcomer", 100, 1, "Keep going")
        ));
        when(achievementDefinitionRepository.findAll()).thenReturn(List.of());

        SubtopicCompletionResult result = service.recordSubtopicCompletion(userId, 100);

        assertThat(profile.getCurrentStreak()).isEqualTo(3);
        assertThat(result.expAwarded()).isEqualTo(105);
        assertThat(result.leveledUp()).isTrue();
        assertThat(result.newLevel()).isEqualTo(2);
        assertThat(profile.getCurrentExp()).isEqualTo(5);
        verify(dailyLearningActivityRepository).save(any(DailyLearningActivity.class));
        verify(learnerProfileRepository).save(profile);
    }

    // UTC-21_TC-04
    @Test
    void recordSubtopicCompletionSecondTimeSameDayDoesNotRecheckDailyAchievements() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = spyProfile(userId);
        profile.setLevel(1);
        profile.setCurrentStreak(3);
        profile.setLastActiveDate(LocalDate.now());

        DailyLearningActivity alreadyLogged = new DailyLearningActivity(profile.getUser(), LocalDate.now());
        alreadyLogged.recordSubtopicCompletion(10);

        when(learnerProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(dailyLearningActivityRepository.findByUserUserIdAndActivityDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.of(alreadyLogged));
        when(streakBonusTierRepository.findAllByOrderByMinStreakDaysAsc())
                .thenReturn(List.of(new StreakBonusTier(3, 5)));
        when(levelDefinitionRepository.findAllByOrderByLevelAsc()).thenReturn(List.of(
                new LevelDefinition(1, "Newcomer", 0, 1, "Start")
        ));
        when(achievementDefinitionRepository.findAll()).thenReturn(List.of());

        service.recordSubtopicCompletion(userId, 10);

        assertThat(profile.getCurrentStreak()).isEqualTo(3);
        verify(achievementDefinitionRepository, org.mockito.Mockito.times(1)).findAll();
    }

    // UTC-21_TC-05
    @Test
    void recordSubtopicCompletionAchievementCascadeUpdatesProfileButNotReturnedLevelFlag() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = spyProfile(userId);
        profile.setLevel(1);
        profile.setLastActiveDate(null);

        AchievementDefinition firstSteps = mock(AchievementDefinition.class);
        when(firstSteps.getId()).thenReturn(1L);
        when(firstSteps.getCriteriaType()).thenReturn(AchievementCriteriaType.FIRST_SUBTOPIC_COMPLETED);
        when(firstSteps.getCriteriaValue()).thenReturn(1);
        when(firstSteps.getExpReward()).thenReturn(20);

        when(learnerProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(dailyLearningActivityRepository.findByUserUserIdAndActivityDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(streakBonusTierRepository.findAllByOrderByMinStreakDaysAsc()).thenReturn(List.of());
        when(levelDefinitionRepository.findAllByOrderByLevelAsc()).thenReturn(List.of(
                new LevelDefinition(1, "Newcomer", 0, 1, "Start"),
                new LevelDefinition(2, "Newcomer", 25, 1, "Keep going")
        ));
        when(achievementDefinitionRepository.findAll()).thenReturn(List.of(firstSteps));
        when(userAchievementRepository.existsByUserUserIdAndAchievementId(userId, 1L)).thenReturn(false);

        SubtopicCompletionResult result = service.recordSubtopicCompletion(userId, 10);

        // Direct XP gain (10) alone does not cross the level-2 threshold (25), so the
        // returned outcome reflects only that first evaluation.
        assertThat(result.leveledUp()).isFalse();
        assertThat(result.newLevel()).isEqualTo(1);
        assertThat(result.unlockedAchievements()).containsExactly(firstSteps);
        // The achievement's +20 XP reward is applied afterward (10 + 20 = 30 >= 25) and
        // actually pushes the persisted profile to level 2, even though the outcome above
        // still reports level 1 / leveledUp=false — this is the observed GamificationService
        // behavior: recordSubtopicCompletion captures LevelUpOutcome before achievement XP.
        assertThat(profile.getLevel()).isEqualTo(2);
        verify(userAchievementRepository).save(any(UserAchievement.class));
    }

    // UTC-22_TC-01
    @Test
    void dailyStreakMaintenanceConsumesShieldWhenLearnerMissedYesterday() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = spyProfile(userId);
        profile.setLevel(2);
        profile.setCurrentStreak(5);
        profile.setCurrentShield(2);
        profile.setLastActiveDate(LocalDate.now().minusDays(2));

        when(learnerProfileRepository.findAll()).thenReturn(List.of(profile));
        when(levelDefinitionRepository.findById(2))
                .thenReturn(Optional.of(new LevelDefinition(2, "Newcomer", 100, 2, "Keep going")));

        service.runDailyStreakMaintenance();

        assertThat(profile.getCurrentShield()).isEqualTo(1);
        assertThat(profile.getStreakSafeThroughDate()).isEqualTo(LocalDate.now().minusDays(1));
        ArgumentCaptor<LearnerEvent> captor = ArgumentCaptor.forClass(LearnerEvent.class);
        verify(learnerEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(LearnerEventType.SHIELD_CONSUMED);
        verify(learnerProfileRepository).save(profile);
    }

    // UTC-22_TC-02
    @Test
    void dailyStreakMaintenanceResetsStreakWhenNoShieldsRemain() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = spyProfile(userId);
        profile.setLevel(2);
        profile.setCurrentStreak(7);
        profile.setCurrentShield(0);
        profile.setLastActiveDate(LocalDate.now().minusDays(3));

        when(learnerProfileRepository.findAll()).thenReturn(List.of(profile));

        service.runDailyStreakMaintenance();

        assertThat(profile.getCurrentStreak()).isEqualTo(0);
        assertThat(profile.getStreakSafeThroughDate()).isNull();
        ArgumentCaptor<LearnerEvent> captor = ArgumentCaptor.forClass(LearnerEvent.class);
        verify(learnerEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(LearnerEventType.STREAK_LOST);
        assertThat(captor.getValue().getStreakDaysLost()).isEqualTo(7);
        assertThat(captor.getValue().getLongestStreak()).isEqualTo(7);
    }

    // UTC-22_TC-03
    @Test
    void dailyStreakMaintenanceSkipsStillActiveAndZeroStreakProfiles() {
        LearnerProfile stillActive = spyProfile(UUID.randomUUID());
        stillActive.setCurrentStreak(3);
        stillActive.setLastActiveDate(LocalDate.now());

        LearnerProfile noStreak = spyProfile(UUID.randomUUID());
        noStreak.setCurrentStreak(0);

        when(learnerProfileRepository.findAll()).thenReturn(List.of(stillActive, noStreak));

        service.runDailyStreakMaintenance();

        verify(learnerEventRepository, never()).save(any());
        verify(learnerProfileRepository, never()).save(any());
    }

    // UTC-22_TC-04
    @Test
    void weeklyShieldRefillRestoresShieldsToLevelMax() {
        UUID userId = UUID.randomUUID();
        LearnerProfile profile = spyProfile(userId);
        profile.setLevel(3);
        profile.setCurrentShield(0);

        when(learnerProfileRepository.findAll()).thenReturn(List.of(profile));
        when(levelDefinitionRepository.findById(3))
                .thenReturn(Optional.of(new LevelDefinition(3, "Scholar", 200, 2, "New rank")));

        service.runWeeklyShieldRefill();

        assertThat(profile.getCurrentShield()).isEqualTo(2);
        assertThat(profile.getShieldPoolLastResetAt()).isNotNull();
        verify(learnerProfileRepository).save(profile);
    }
}
