package com.example.demo.dto.course;

import java.time.Instant;

public record CourseReviewCommentDto(
        Long id,
        Long moduleId,
        Long subTopicId,
        String feedback,
        Instant createdAt,
        Instant updatedAt
) {
}
