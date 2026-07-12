package com.example.demo.assessment.dto;

import java.util.List;

public record AssessmentQuestionDto(
        Long id,
        String questionText,
        List<AssessmentOptionDto> options
) {
}
