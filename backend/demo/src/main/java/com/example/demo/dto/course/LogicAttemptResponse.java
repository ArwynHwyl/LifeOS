package com.example.demo.dto.course;

import com.example.demo.entity.course.InteractiveProgressStatus;
import java.time.Instant;
import java.util.Map;

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
