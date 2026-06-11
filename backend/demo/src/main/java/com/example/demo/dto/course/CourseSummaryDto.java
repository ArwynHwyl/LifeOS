package com.example.demo.dto.course;

import com.example.demo.entity.course.CourseStatus;
import java.time.Instant;
import java.util.UUID;

public record CourseSummaryDto(
        Long id,
        String title,
        String description,
        String coverId,
        CourseStatus status,
        UUID createdById,
        String createdByName,
        UUID approvedById,
        String approvedByName,
        Instant publishedAt,
        int moduleCount,
        Instant createdAt,
        Instant updatedAt
) {
}
