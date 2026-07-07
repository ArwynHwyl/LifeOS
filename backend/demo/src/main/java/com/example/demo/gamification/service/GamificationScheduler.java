package com.example.demo.gamification.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GamificationScheduler {

    private final GamificationService gamificationService;

    public GamificationScheduler(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    // Once a day: consume a shield or break the streak for anyone with no activity yesterday.
    @Scheduled(cron = "0 10 0 * * *")
    public void dailyStreakMaintenance() {
        gamificationService.runDailyStreakMaintenance();
    }

    // Once a week: refill every learner's shield pool back up to their level's max.
    @Scheduled(cron = "0 15 0 * * MON")
    public void weeklyShieldRefill() {
        gamificationService.runWeeklyShieldRefill();
    }
}
