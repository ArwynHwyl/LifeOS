package com.example.demo.course.dto.document.response;

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
