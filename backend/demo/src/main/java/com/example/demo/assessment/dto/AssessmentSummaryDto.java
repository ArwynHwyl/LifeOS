package com.example.demo.assessment.dto;

public record AssessmentSummaryDto(
        Long id,
        String title,
        String description,
        String tag,
        long questionCount,
        int sortOrder,
        boolean attempted,
        Integer score,
        Integer totalQuestions
) {
}
