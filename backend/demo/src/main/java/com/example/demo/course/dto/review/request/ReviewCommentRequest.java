package com.example.demo.course.dto.review.request;

import jakarta.validation.constraints.NotBlank;

public record ReviewCommentRequest(
        Long moduleId,
        Long subTopicId,
        @NotBlank String feedback
) {
}
