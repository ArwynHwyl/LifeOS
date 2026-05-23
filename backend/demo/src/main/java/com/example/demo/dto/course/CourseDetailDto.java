package com.example.demo.dto.course;

import com.example.demo.entity.course.CourseStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseDetailDto(
        Long id,
        String title,
        String description,
        CourseStatus status,
        UUID createdById,
        String createdByName,
        UUID approvedById,
        String approvedByName,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<ModuleDto> modules,
        List<DocumentSourceDto> documentSources
) {
}
