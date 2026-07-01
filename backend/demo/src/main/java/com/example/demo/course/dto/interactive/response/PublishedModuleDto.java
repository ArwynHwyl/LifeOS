package com.example.demo.course.dto.interactive.response;

import java.util.List;

import com.example.demo.course.entity.InteractionType;

public record PublishedModuleDto(
        Long id,
        String title,
        String description,
        Integer sortOrder,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig,
        List<PublishedSubTopicDto> subTopics
) {
}
