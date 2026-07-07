package com.example.demo.course.dto.management.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import com.example.demo.course.entity.ContentDepth;
import com.example.demo.course.entity.InteractionType;

public record ModuleCreateRequest(
        @NotBlank String title,
        String description,
        @NotNull @Min(0) Integer sortOrder,
        @NotNull ContentDepth contentDepth,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig,
        @Valid List<SubTopicCreateRequest> subTopics
) {
}
