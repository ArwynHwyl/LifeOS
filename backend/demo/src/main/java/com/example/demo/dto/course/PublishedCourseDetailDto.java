package com.example.demo.dto.course;

import java.time.Instant;
import java.util.List;

public record PublishedCourseDetailDto(
        Long id,
        String title,
        String description,
        String coverId,
        Instant publishedAt,
        List<PublishedModuleDto> modules
) {
}
