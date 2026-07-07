package com.example.demo.course.service.generation.model;

import com.example.demo.course.entity.InteractionType;

public record GeneratedSubTopicDraft(
        String title,
        String content,
        String mascotPrompt,
        InteractionType interactionType,
        String interactionPrompt,
        String interactionConfig
) {
    public GeneratedSubTopicDraft(String title, String content) {
        this(title, content, null, InteractionType.NONE, null, null);
    }

    public GeneratedSubTopicDraft(
            String title,
            String content,
            InteractionType interactionType,
            String interactionPrompt,
            String interactionConfig
    ) {
        this(title, content, null, interactionType, interactionPrompt, interactionConfig);
    }
}
