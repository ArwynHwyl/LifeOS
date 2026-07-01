package com.example.demo.course.dto.interactive.response;

import com.example.demo.course.entity.InteractionType;

public record PublishedSubTopicDto(
        Long id,
        String title,
        String content,
        String contentHtml,
        String mascotPrompt,
        Integer sortOrder,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig,
        InteractiveProgressDto interactiveProgress
) {
}
