package com.example.demo.dto.course;

import com.example.demo.entity.course.InteractionType;
import com.example.demo.entity.course.SubTopicSourceType;
import java.time.Instant;
import java.util.List;

public record SubTopicDto(
        Long id,
        Long moduleId,
        String title,
        String content,
        String contentHtml,
        String mascotPrompt,
        List<SubTopicAssetDto> assets,
        Integer sortOrder,
        SubTopicSourceType sourceType,
        Integer pageStart,
        Integer pageEnd,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig,
        Instant createdAt,
        Instant updatedAt
) {
}
