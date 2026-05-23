package com.example.demo.service.course;

import com.example.demo.entity.course.InteractionType;
import java.util.List;

public record GeneratedCourseModuleDraft(
        String title,
        String description,
        InteractionType interactionType,
        String interactionPrompt,
        List<GeneratedSubTopicDraft> subTopics
) {
}
