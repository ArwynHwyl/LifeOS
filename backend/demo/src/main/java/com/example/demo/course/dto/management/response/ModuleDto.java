package com.example.demo.course.dto.management.response;

import java.time.Instant;
import java.util.List;

import com.example.demo.course.entity.ContentDepth;
import com.example.demo.course.entity.InteractionType;

public record ModuleDto(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer sortOrder,
        ContentDepth contentDepth,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig,
        Instant createdAt,
        Instant updatedAt,
        List<SubTopicDto> subTopics
) {
}
