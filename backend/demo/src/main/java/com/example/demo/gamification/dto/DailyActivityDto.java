package com.example.demo.gamification.dto;

import java.time.LocalDate;

public record DailyActivityDto(
        LocalDate date,
        int subtopicsCompleted,
        int expEarned
) {
}
