package com.example.demo.dto.course;

import jakarta.validation.constraints.NotBlank;

public record CourseUpdateRequest(
        @NotBlank String title,
        String description
) {
}
