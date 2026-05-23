package com.example.demo.dto.course;

import java.time.Instant;
import java.util.Map;

public record DocumentUploadUrlResponse(
        String uploadUrl,
        String storagePath,
        Instant expiresAt,
        String method,
        Map<String, String> headers
) {
}
