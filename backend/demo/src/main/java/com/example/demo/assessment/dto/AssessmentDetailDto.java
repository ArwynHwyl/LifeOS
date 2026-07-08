package com.example.demo.assessment.dto;

import java.util.List;

public record AssessmentDetailDto(
        Long id,
        String title,
        String description,
        boolean attempted,
        List<AssessmentQuestionDto> questions,
        AssessmentResultDto result
) {
}
