package com.example.demo.course.dto.interactive.response;

import java.time.Instant;
import java.util.Map;

import com.example.demo.course.entity.InteractionType;
import com.example.demo.course.entity.InteractiveProgressStatus;

public record InteractiveAttemptResponse(
        Long subTopicId,
        InteractionType interactionType,
        boolean correct,
        InteractiveProgressStatus status,
        int attemptCount,
        Instant masteredAt,
        Instant updatedAt,
        String feedback,
        Map<String, Object> details
) {
}

