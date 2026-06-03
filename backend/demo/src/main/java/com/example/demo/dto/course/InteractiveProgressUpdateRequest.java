package com.example.demo.dto.course;

import com.example.demo.entity.course.InteractiveProgressStatus;
import jakarta.validation.constraints.NotNull;

public record InteractiveProgressUpdateRequest(
        @NotNull InteractiveProgressStatus status
) {
}
