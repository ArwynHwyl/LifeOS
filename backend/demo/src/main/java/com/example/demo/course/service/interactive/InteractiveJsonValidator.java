package com.example.demo.course.service.interactive;

import com.example.demo.shared.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class InteractiveJsonValidator {

    private InteractiveJsonValidator() {
    }

    static void rejectUnknownFields(ObjectNode node, Set<String> allowedFields, String path) {
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowedFields.contains(field)) {
                throw new ValidationException(path + " contains unsupported field: " + field);
            }
        }
    }

    static String requiredText(ObjectNode node, String fieldName, int maxLength) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        String text = value.asText().trim();
        if (text.length() > maxLength) {
            throw new ValidationException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        node.put(fieldName, text);
        return text;
    }

    static String optionalText(ObjectNode node, String fieldName, int maxLength) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new ValidationException(fieldName + " must be text");
        }
        String text = value.asText().trim();
        if (text.length() > maxLength) {
            throw new ValidationException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        node.put(fieldName, text);
        return text;
    }

    static double requiredNumber(ObjectNode node, String fieldName, double min, double max) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isNumber()) {
            throw new ValidationException(fieldName + " is required");
        }
        double number = value.asDouble();
        if (!Double.isFinite(number) || number < min || number > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max);
        }
        return number;
    }

    static Double optionalNumber(ObjectNode node, String fieldName, double min, double max) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber()) {
            throw new ValidationException(fieldName + " must be a number");
        }
        double number = value.asDouble();
        if (!Double.isFinite(number) || number < min || number > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max);
        }
        return number;
    }

    static int requiredInteger(ObjectNode node, String fieldName, int min, int max) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.canConvertToInt() || value.asDouble() % 1 != 0) {
            throw new ValidationException(fieldName + " must be an integer");
        }
        int number = value.asInt();
        if (number < min || number > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max);
        }
        return number;
    }

    static void validateVisualBounds(ObjectNode node, double canvasWidth, double canvasHeight) {
        double x = requiredNumber(node, "x", 0, canvasWidth);
        double y = requiredNumber(node, "y", 0, canvasHeight);
        double width = requiredNumber(node, "width", 8, canvasWidth);
        double height = requiredNumber(node, "height", 8, canvasHeight);
        if (x + width > canvasWidth || y + height > canvasHeight) {
            throw new ValidationException("visual object must stay inside canvas bounds");
        }
    }

    static String requiredId(ObjectNode node, String fieldName, String label) {
        String id = requiredText(node, fieldName, 60);
        if (!id.matches("[A-Za-z][A-Za-z0-9_-]{0,59}")) {
            throw new ValidationException(label + " must start with a letter and contain only letters, numbers, underscores, or dashes");
        }
        return id;
    }

    static String requiredLongId(ObjectNode node, String fieldName, String label, int maxLength) {
        String id = requiredText(node, fieldName, maxLength);
        if (!id.matches("[A-Za-z][A-Za-z0-9_-]{0," + (maxLength - 1) + "}")) {
            throw new ValidationException(label + " must start with a letter and contain only letters, numbers, underscores, or dashes");
        }
        return id;
    }

    static List<String> requiredIdArray(ObjectNode node, String fieldName, String label, int min, int max) {
        JsonNode values = node.get(fieldName);
        if (values == null || !values.isArray() || values.size() < min || values.size() > max) {
            throw new ValidationException(label + " must contain " + min + " to " + max + " ids");
        }
        List<String> ids = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isTextual()) {
                throw new ValidationException(label + " must contain ids");
            }
            String id = value.asText().trim();
            if (!id.matches("[A-Za-z][A-Za-z0-9_-]{0,59}")) {
                throw new ValidationException(label + " entries must start with a letter and contain only letters, numbers, underscores, or dashes");
            }
            ids.add(id);
        }
        return ids;
    }
}
