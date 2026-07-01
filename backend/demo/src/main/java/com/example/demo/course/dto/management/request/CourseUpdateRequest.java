package com.example.demo.course.dto.management.request;

import jakarta.validation.constraints.NotBlank;

public record CourseUpdateRequest(
        @NotBlank String title,
        String description,
        String coverId
) {
}
