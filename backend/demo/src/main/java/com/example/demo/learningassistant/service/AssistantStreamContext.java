package com.example.demo.learningassistant.service;

import com.example.demo.learningassistant.entity.AssistantMode;

public record AssistantStreamContext(
        Long conversationId, Long userMessageId, Long assistantMessageId,
        AssistantMode mode, String message, String selectedText,
        String courseTitle, String moduleTitle, String subTopicTitle, String lessonText,
        String mascotPrompt, String interactionPrompt, String interactionConfig) {}
