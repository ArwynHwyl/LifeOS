package com.example.demo.learningassistant.service;

import com.example.demo.learningassistant.entity.AssistantMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AssistantResponseFormatter {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "teachingPoints", "examples", "followUpQuestions");
    private static final int MAX_TEACHING_POINT_LENGTH = 1_200;
    private static final int MAX_EXAMPLE_LENGTH = 600;
    private static final int MAX_QUESTION_LENGTH = 300;

    private final ObjectMapper objectMapper;

    public AssistantResponseFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String format(AssistantMode mode, String rawResponse) {
        JsonNode response = parse(rawResponse);
        if (!response.isObject() || !hasOnlyAllowedFields(response)) {
            throw invalid("AI response did not match the required object shape");
        }

        String teachingPoint = singleRequiredItem(response, "teachingPoints", MAX_TEACHING_POINT_LENGTH);
        String example = singleOptionalItem(response, "examples", MAX_EXAMPLE_LENGTH);
        String followUpQuestion = singleRequiredItem(response, "followUpQuestions", MAX_QUESTION_LENGTH);

        if (containsQuestionMark(teachingPoint) || containsQuestionMark(example)) {
            throw invalid("Only followUpQuestions may contain a question");
        }
        if (mode == AssistantMode.HINT && example != null) {
            throw invalid("Hint responses cannot include an example");
        }
        if (!isExactlyOneQuestion(followUpQuestion)) {
            throw invalid("The response must end with exactly one question");
        }

        StringBuilder answer = new StringBuilder(teachingPoint);
        if (example != null) answer.append("\n\nExample: ").append(example);
        answer.append("\n\n").append(followUpQuestion);
        return answer.toString();
    }

    private JsonNode parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) throw invalid("AI returned an empty response");
        try {
            return objectMapper.readTree(rawResponse);
        } catch (Exception exception) {
            throw new InvalidAssistantResponseException("AI response was not valid JSON", exception);
        }
    }

    private boolean hasOnlyAllowedFields(JsonNode response) {
        if (!response.has("teachingPoints") || !response.has("examples") || !response.has("followUpQuestions")) {
            return false;
        }
        var names = response.fieldNames();
        while (names.hasNext()) {
            if (!ALLOWED_FIELDS.contains(names.next())) return false;
        }
        return true;
    }

    private String singleRequiredItem(JsonNode response, String field, int maxLength) {
        JsonNode value = response.get(field);
        if (value == null || !value.isArray() || value.size() != 1 || !value.get(0).isTextual()) {
            throw invalid(field + " must contain exactly one text item");
        }
        String normalized = normalize(value.get(0).asText());
        if (normalized.isEmpty()) throw invalid(field + " must not be blank");
        if (normalized.length() > maxLength) throw invalid(field + " is too long");
        return normalized;
    }

    private String singleOptionalItem(JsonNode response, String field, int maxLength) {
        JsonNode value = response.get(field);
        if (value == null || !value.isArray() || value.size() > 1) {
            throw invalid(field + " must contain zero or one text item");
        }
        if (value.isEmpty()) return null;
        if (!value.get(0).isTextual()) throw invalid(field + " item must be text");
        String normalized = normalize(value.get(0).asText());
        if (normalized.isEmpty()) return null;
        if (normalized.length() > maxLength) throw invalid(field + " is too long");
        return normalized;
    }

    private String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private boolean containsQuestionMark(String value) {
        return value != null && value.indexOf('?') >= 0;
    }

    private boolean isExactlyOneQuestion(String value) {
        return value.endsWith("?") && value.chars().filter(character -> character == '?').count() == 1;
    }

    private InvalidAssistantResponseException invalid(String message) {
        return new InvalidAssistantResponseException(message);
    }

    static final class InvalidAssistantResponseException extends RuntimeException {
        InvalidAssistantResponseException(String message) {
            super(message);
        }

        InvalidAssistantResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
