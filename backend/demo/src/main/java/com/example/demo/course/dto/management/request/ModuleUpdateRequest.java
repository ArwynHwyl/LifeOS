package com.example.demo.course.dto.management.request;

import com.example.demo.course.entity.ContentDepth;
import com.example.demo.course.entity.InteractionType;

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
