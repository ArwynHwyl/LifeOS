package com.example.demo.dto.course;

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
