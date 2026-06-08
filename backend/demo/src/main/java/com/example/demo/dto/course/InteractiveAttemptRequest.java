package com.example.demo.dto.course;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public record InteractiveAttemptRequest(
        JsonNode answer,
        Map<String, Double> values,
        Map<String, Double> regionAnswers
) {
}

