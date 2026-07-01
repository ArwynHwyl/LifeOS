package com.example.demo.course.dto.management.response;

import java.time.Instant;
import java.util.UUID;

import com.example.demo.course.entity.CourseStatus;

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
