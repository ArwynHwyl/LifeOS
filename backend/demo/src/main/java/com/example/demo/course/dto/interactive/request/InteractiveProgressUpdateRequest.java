package com.example.demo.course.dto.interactive.request;

import com.example.demo.course.entity.InteractiveProgressStatus;

import jakarta.validation.constraints.NotNull;

public record InteractiveProgressUpdateRequest(
        @NotNull InteractiveProgressStatus status
) {
}
