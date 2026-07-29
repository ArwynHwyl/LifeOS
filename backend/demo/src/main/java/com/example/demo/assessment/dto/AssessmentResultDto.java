package com.example.demo.assessment.dto;

import com.example.demo.gamification.dto.GamificationRewardDto;
import java.time.Instant;
import java.util.List;

public record AssessmentResultDto(
        Long assessmentId,
        String title,
        int score,
        int totalQuestions,
        double percentage,
        Instant submittedAt,
        List<AssessmentQuestionResultDto> questionResults,
        GamificationRewardDto reward
) {
}
