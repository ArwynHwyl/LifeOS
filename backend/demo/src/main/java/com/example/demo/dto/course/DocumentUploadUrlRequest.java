package com.example.demo.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentUploadUrlRequest(
        @NotBlank String fileName,
        @NotBlank String fileType,
        @NotNull @Min(1) Long fileSizeBytes
) {
}
