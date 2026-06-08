package com.example.demo.service.course;

import com.example.demo.entity.course.InteractionType;

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
