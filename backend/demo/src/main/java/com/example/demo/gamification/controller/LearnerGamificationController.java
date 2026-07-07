package com.example.demo.gamification.controller;

import com.example.demo.gamification.dto.AchievementDto;
import com.example.demo.gamification.dto.DailyActivityDto;
import com.example.demo.gamification.dto.LearnerProfileSummaryDto;
import com.example.demo.gamification.dto.LevelRoadmapEntryDto;
import com.example.demo.gamification.dto.PendingLearnerEventDto;
import com.example.demo.gamification.service.GamificationService;
import com.example.demo.shared.security.CurrentUser;
import com.example.demo.user.entity.User;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learner/gamification")
@PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'ADMIN')")
public class LearnerGamificationController {

    private final GamificationService gamificationService;

    public LearnerGamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/profile")
    public LearnerProfileSummaryDto getProfile(@AuthenticationPrincipal User user) {
        return gamificationService.getProfileSummary(CurrentUser.id(user));
    }

    @GetMapping("/levels")
    public List<LevelRoadmapEntryDto> getLevelRoadmap(@AuthenticationPrincipal User user) {
        return gamificationService.getLevelRoadmapDto(CurrentUser.id(user));
    }

    @GetMapping("/achievements")
    public List<AchievementDto> getAchievements(@AuthenticationPrincipal User user) {
        return gamificationService.getAchievementCatalogDto(CurrentUser.id(user));
    }

    @GetMapping("/notifications/pending")
    public List<PendingLearnerEventDto> getPendingNotifications(@AuthenticationPrincipal User user) {
        return gamificationService.getPendingEventDtos(CurrentUser.id(user));
    }

    @PostMapping("/notifications/{eventId}/ack")
    public void acknowledgeNotification(@AuthenticationPrincipal User user, @PathVariable Long eventId) {
        gamificationService.acknowledgeEvent(CurrentUser.id(user), eventId);
    }

    @GetMapping("/activity")
    public List<DailyActivityDto> getRecentActivity(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "7") int days
    ) {
        return gamificationService.getRecentActivityDto(CurrentUser.id(user), days);
    }
}
