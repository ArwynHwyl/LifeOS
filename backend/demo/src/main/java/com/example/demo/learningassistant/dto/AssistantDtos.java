package com.example.demo.learningassistant.dto;

import com.example.demo.learningassistant.entity.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AssistantDtos {
    private AssistantDtos() {}

    public record SendMessageRequest(
            @NotNull AssistantMode mode,
            @Size(max = 1000) String message,
            @Size(max = 50) String suggestionKey,
            @Size(max = 2000) String selectedText) {}

    public record FeedbackRequest(@NotNull AssistantFeedback feedback) {}
    public record FeedbackResponse(Long messageId, AssistantFeedback feedback) {}
    public record SuggestionDto(String key, String label, AssistantMode mode, String message) {}
    public record MessageDto(Long id, AssistantMessageRole role, AssistantMode mode, String content,
                             String selectedText, String suggestionKey, AssistantMessageStatus status,
                             AssistantFeedback feedback, Instant createdAt) {}
    public record ContextDto(Long courseId, String courseTitle, Long moduleId, String moduleTitle,
                             Long subTopicId, String subTopicTitle) {}
    public record ConversationDto(Long conversationId, String greeting, ContextDto context,
                                  List<SuggestionDto> suggestions, List<MessageDto> messages) {}
    public record WeaknessDto(Long courseId, String courseTitle, Long moduleId, String moduleTitle,
                              Long subTopicId, String subTopicTitle, long questionCount,
                              long notUnderstoodCount, Instant lastAskedAt) {}
}
