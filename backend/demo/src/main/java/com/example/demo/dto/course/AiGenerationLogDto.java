package com.example.demo.dto.course;

import com.example.demo.entity.course.AiGenerationStatus;
import com.example.demo.entity.course.AiGenerationType;
import java.time.Instant;
import java.util.UUID;

public record AiGenerationLogDto(
        Long id,
        Long courseId,
        Long moduleId,
        AiGenerationType type,
        UUID requestedById,
        Long documentSourceId,
        Integer pageStart,
        Integer pageEnd,
        String requirements,
        String prompt,
        AiGenerationStatus status,
        String rawResponse,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
