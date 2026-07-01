package com.example.demo.course.dto.generation.response;

import java.time.Instant;
import java.util.UUID;

import com.example.demo.course.entity.AiGenerationStatus;
import com.example.demo.course.entity.AiGenerationType;

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
