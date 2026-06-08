package com.example.demo.dto.course;

import com.example.demo.entity.course.InteractionType;
import com.example.demo.entity.course.InteractiveProgressStatus;
import java.time.Instant;
import java.util.Map;

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

