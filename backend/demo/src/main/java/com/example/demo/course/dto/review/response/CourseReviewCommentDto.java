package com.example.demo.course.dto.review.response;

import java.time.Instant;

public record CourseReviewCommentDto(
        Long id,
        Long moduleId,
        Long subTopicId,
        String feedback,
        Instant createdAt,
        Instant updatedAt,
        boolean resolved,
        Instant resolvedAt
) {
}
