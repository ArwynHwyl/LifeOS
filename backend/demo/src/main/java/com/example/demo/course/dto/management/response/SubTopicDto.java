package com.example.demo.course.dto.management.response;

import com.example.demo.course.dto.interactive.response.SubTopicAssetDto;
import java.time.Instant;
import java.util.List;

import com.example.demo.course.entity.InteractionType;
import com.example.demo.course.entity.SubTopicSourceType;

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
