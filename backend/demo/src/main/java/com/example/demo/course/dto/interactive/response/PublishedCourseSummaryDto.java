package com.example.demo.course.dto.interactive.response;

import java.time.Instant;

public record PublishedCourseSummaryDto(
        Long id,
        String title,
        String description,
        String coverId,
        Instant publishedAt
) {
}
