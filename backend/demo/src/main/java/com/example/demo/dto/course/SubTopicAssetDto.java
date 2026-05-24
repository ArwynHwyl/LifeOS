package com.example.demo.dto.course;

import java.time.Instant;

public record SubTopicAssetDto(
        Long id,
        Long subTopicId,
        String fileName,
        String fileType,
        Long fileSizeBytes,
        String altText,
        String storagePath,
        String fileUrl,
        Instant fileUrlExpiresAt,
        Instant createdAt
) {
}
