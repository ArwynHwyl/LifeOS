package com.example.demo.course.dto.review.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.demo.course.entity.CourseReviewDecision;

public record CourseReviewDto(
        Long id,
        Long courseId,
        UUID reviewerId,
        String reviewerName,
        CourseReviewDecision decision,
        String feedback,
        Instant createdAt,
        Instant updatedAt,
        List<CourseReviewCommentDto> comments
) {

    public CourseReviewDto(
            Long id,
            Long courseId,
            UUID reviewerId,
            String reviewerName,
            CourseReviewDecision decision,
            String feedback,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, courseId, reviewerId, reviewerName, decision, feedback, createdAt, updatedAt, List.of());
    }
}
