package com.example.demo.course.dto.management.request;

import com.example.demo.course.entity.InteractionType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubTopicUpdateRequest(
        @NotBlank String title,
        String content,
        String mascotPrompt,
        @NotNull @Min(0) Integer sortOrder,
        Integer pageStart,
        Integer pageEnd,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig
) {
}
