package com.example.demo.course.dto.interactive.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubTopicImageCompleteRequest(
        @NotBlank String fileName,
        @NotBlank String fileType,
        @NotNull Long fileSizeBytes,
        @NotBlank String storagePath,
        String altText
) {
}
