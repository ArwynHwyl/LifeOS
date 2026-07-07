package com.example.demo.course.dto.interactive.response;

import java.time.Instant;
import java.util.Map;

import com.example.demo.course.entity.InteractiveProgressStatus;

public record LogicAttemptResponse(
        Long subTopicId,
        String kind,
        boolean correct,
        InteractiveProgressStatus status,
        int attemptCount,
        Instant masteredAt,
        Instant updatedAt,
        String feedback,
        Map<String, Object> details
) {
}
