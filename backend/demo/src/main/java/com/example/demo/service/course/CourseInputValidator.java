package com.example.demo.service.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.service.exception.ValidationException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CourseInputValidator {

    private final ObjectMapper objectMapper;

    CourseInputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String requiredText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        return trimmed;
    }

    String optionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        return trimmed.isBlank() ? null : trimmed;
    }

    String optionalJson(String value, String fieldName, int maxLength) {
        String trimmed = optionalText(value, fieldName, maxLength);
        if (trimmed == null) {
            return null;
        }
        try {
            objectMapper.readTree(trimmed);
            return trimmed;
        } catch (java.io.IOException ex) {
            throw new ValidationException(fieldName + " must be valid JSON");
        }
    }

    Long requiredId(Long value, String fieldName) {
        if (value == null || value < 1) {
            throw new ValidationException(fieldName + " is required");
        }
        return value;
    }

    UUID requiredUserId(UUID value) {
        if (value == null) {
            throw new ValidationException("userId is required");
        }
        return value;
    }

    Integer requiredSortOrder(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new ValidationException(fieldName + " must be zero or greater");
        }
        return value;
    }

    Long requiredPositiveLong(Long value, String fieldName) {
        if (value == null || value < 1) {
            throw new ValidationException(fieldName + " must be greater than zero");
        }
        return value;
    }

    Integer optionalPositiveInteger(Integer value, String fieldName) {
        if (value != null && value < 1) {
            throw new ValidationException(fieldName + " must be greater than zero");
        }
        return value;
    }

    void validateOptionalPageRange(Integer pageStart, Integer pageEnd) {
        if (pageStart == null && pageEnd == null) {
            return;
        }
        validateRequiredPageRange(pageStart, pageEnd, null);
    }

    void validateRequiredPageRange(Integer pageStart, Integer pageEnd, Integer pageCount) {
        if (pageStart == null || pageEnd == null || pageStart < 1 || pageEnd < pageStart) {
            throw new ValidationException("Page range must include pageStart and pageEnd with pageEnd >= pageStart");
        }
        if (pageCount != null && pageEnd > pageCount) {
            throw new ValidationException("Page range exceeds document page count");
        }
    }
}
