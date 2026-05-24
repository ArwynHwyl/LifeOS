package com.example.demo.dto.course;

import jakarta.validation.constraints.NotBlank;

public record ReviewCommentRequest(
        Long moduleId,
        Long subTopicId,
        @NotBlank String feedback
) {
}
