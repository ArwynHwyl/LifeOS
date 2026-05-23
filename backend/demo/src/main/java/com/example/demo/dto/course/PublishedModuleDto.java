package com.example.demo.dto.course;

import com.example.demo.entity.course.InteractionType;
import java.util.List;

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
