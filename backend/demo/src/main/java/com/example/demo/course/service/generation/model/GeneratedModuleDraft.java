package com.example.demo.course.service.generation.model;

import java.util.List;

public record GeneratedModuleDraft(
        String rawResponse,
        List<GeneratedSubTopicDraft> subTopics
) {
}
