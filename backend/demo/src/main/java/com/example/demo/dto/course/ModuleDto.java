package com.example.demo.dto.course;

import com.example.demo.entity.course.ContentDepth;
import com.example.demo.entity.course.InteractionType;
import java.time.Instant;
import java.util.List;

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
