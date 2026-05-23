package com.example.demo.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AiGenerationRequest(
        @NotNull Long documentSourceId,
        @Min(1) Integer pageStart,
        @Min(1) Integer pageEnd,
        String requirements,
        String prompt
) {
}
