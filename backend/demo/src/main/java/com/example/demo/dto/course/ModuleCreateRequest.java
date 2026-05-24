package com.example.demo.dto.course;

import com.example.demo.entity.course.ContentDepth;
import com.example.demo.entity.course.InteractionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

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
