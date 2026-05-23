package com.example.demo.dto.course;

import com.example.demo.entity.course.InteractionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubTopicCreateRequest(
        @NotBlank String title,
        String content,
        @NotNull @Min(0) Integer sortOrder,
        Integer pageStart,
        Integer pageEnd,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig
) {
}
