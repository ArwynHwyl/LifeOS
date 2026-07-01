package com.example.demo.course.dto.document.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentSourceDto(
        Long id,
        Long courseId,
        String fileName,
        String displayName,
        String fileType,
        Long fileSizeBytes,
        String storagePath,
        UUID uploadedById,
        String uploadedByName,
        Integer pageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
