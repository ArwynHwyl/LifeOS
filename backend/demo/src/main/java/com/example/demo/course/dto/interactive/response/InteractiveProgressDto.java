package com.example.demo.course.dto.interactive.response;

import java.time.Instant;

import com.example.demo.course.entity.InteractiveProgressStatus;

public record InteractiveProgressDto(
        Long subTopicId,
        InteractiveProgressStatus status,
        int attemptCount,
        Instant masteredAt,
        Instant updatedAt
) {
}
