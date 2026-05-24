package com.example.demo.dto.course;

import com.example.demo.entity.course.CourseReviewDecision;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
