package com.example.demo.course.dto.management.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.demo.course.entity.CourseStatus;
import com.example.demo.course.dto.document.response.DocumentSourceDto;
import com.example.demo.course.dto.management.response.ModuleDto;

public record CourseDetailDto(
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
        Instant createdAt,
        Instant updatedAt,
        List<ModuleDto> modules,
        List<DocumentSourceDto> documentSources
) {
}
