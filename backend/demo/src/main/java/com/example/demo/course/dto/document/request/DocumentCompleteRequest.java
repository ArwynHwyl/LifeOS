package com.example.demo.course.dto.document.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentCompleteRequest(
        @NotBlank String fileName,
        @NotBlank String displayName,
        @NotBlank String fileType,
        @NotNull @Min(1) Long fileSizeBytes,
        @NotBlank String storagePath
) {
}
