package com.example.demo.dto.course;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public record LogicAttemptRequest(
        String kind,
        JsonNode answer,
        Map<String, Boolean> inputs,
        List<LogicStepSubmissionDto> steps
) {
}
