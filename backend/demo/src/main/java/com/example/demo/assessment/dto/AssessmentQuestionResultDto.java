package com.example.demo.assessment.dto;

import java.util.List;

public record AssessmentQuestionResultDto(
        Long id,
        String questionText,
        boolean correct,
        List<AssessmentOptionResultDto> options
) {
}
