package com.example.demo.dto.course;

import com.example.demo.entity.course.InteractiveProgressStatus;
import java.time.Instant;

public record InteractiveProgressDto(
        Long subTopicId,
        InteractiveProgressStatus status,
        int attemptCount,
        Instant masteredAt,
        Instant updatedAt
) {
}
