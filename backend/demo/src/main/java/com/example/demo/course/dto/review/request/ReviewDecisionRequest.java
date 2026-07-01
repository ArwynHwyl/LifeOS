package com.example.demo.course.dto.review.request;

import jakarta.validation.Valid;
import java.util.List;

public record ReviewDecisionRequest(String feedback, @Valid List<ReviewCommentRequest> comments) {

    public ReviewDecisionRequest(String feedback) {
        this(feedback, List.of());
    }
}
