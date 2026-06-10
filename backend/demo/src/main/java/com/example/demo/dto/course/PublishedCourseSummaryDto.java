package com.example.demo.dto.course;

import java.time.Instant;

public record PublishedCourseSummaryDto(
        Long id,
        String title,
        String description,
        String coverId,
        Instant publishedAt
) {
}
