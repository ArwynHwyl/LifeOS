package com.example.demo.assessment.dto;

public record AssessmentOptionResultDto(
        Long id,
        String optionText,
        boolean correct,
        boolean selected
) {
}
