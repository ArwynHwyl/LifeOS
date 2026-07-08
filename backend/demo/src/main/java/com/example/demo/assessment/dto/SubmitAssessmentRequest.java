package com.example.demo.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubmitAssessmentRequest(
        @NotEmpty @Valid List<AnswerSubmission> answers
) {
}
