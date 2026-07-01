package com.example.demo.course.service.generation.model;

import java.util.List;

import com.example.demo.course.entity.InteractionType;

public record GeneratedCourseModuleDraft(
        String title,
        String description,
        InteractionType interactionType,
        String interactionPrompt,
        List<GeneratedSubTopicDraft> subTopics
) {
}
