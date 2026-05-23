package com.example.demo.dto.course;

import java.time.Instant;
import java.util.List;

public record DocumentPreviewResponse(
        Long documentId,
        String fileUrl,
        Instant fileUrlExpiresAt,
        Integer pageStart,
        Integer pageEnd,
        Integer pageCount,
        List<DocumentPreviewPageDto> pages
) {
}
