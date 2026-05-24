package com.example.demo.dto.course;

import com.example.demo.entity.course.ContentDepth;
import com.example.demo.entity.course.InteractionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModuleUpdateRequest(
        @NotBlank String title,
        String description,
        @NotNull @Min(0) Integer sortOrder,
        @NotNull ContentDepth contentDepth,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig
) {
}
