package com.example.demo.course.dto.interactive.request;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public record InteractiveAttemptRequest(
        JsonNode answer,
        Map<String, Double> values,
        Map<String, Double> regionAnswers
) {
}

