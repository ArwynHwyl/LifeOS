package com.example.demo.dto.course;

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
