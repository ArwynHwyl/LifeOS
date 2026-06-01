package com.example.demo.dto.course;

import com.example.demo.entity.course.InteractionType;

public record PublishedSubTopicDto(
        Long id,
        String title,
        String content,
        String contentHtml,
        Integer sortOrder,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig
) {
}
