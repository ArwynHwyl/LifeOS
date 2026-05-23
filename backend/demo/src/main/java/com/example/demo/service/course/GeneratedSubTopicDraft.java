package com.example.demo.service.course;

import com.example.demo.entity.course.InteractionType;

public record GeneratedSubTopicDraft(
        String title,
        String content,
        InteractionType interactionType,
        String interactionPrompt
) {
    public GeneratedSubTopicDraft(String title, String content) {
        this(title, content, InteractionType.NONE, null);
    }
}
