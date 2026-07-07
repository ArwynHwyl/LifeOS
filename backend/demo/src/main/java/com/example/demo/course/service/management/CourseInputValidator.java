package com.example.demo.course.service.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.shared.exception.ValidationException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CourseInputValidator {

    private final ObjectMapper objectMapper;

    public CourseInputValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String requiredText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        return trimmed;
    }

    public String optionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        return trimmed.isBlank() ? null : trimmed;
    }

    public String optionalJson(String value, String fieldName, int maxLength) {
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

    public Long requiredId(Long value, String fieldName) {
        if (value == null || value < 1) {
            throw new ValidationException(fieldName + " is required");
        }
        return value;
    }

    public UUID requiredUserId(UUID value) {
        if (value == null) {
            throw new ValidationException("userId is required");
        }
        return value;
    }

    public Integer requiredSortOrder(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new ValidationException(fieldName + " must be zero or greater");
        }
        return value;
    }

    public Long requiredPositiveLong(Long value, String fieldName) {
        if (value == null || value < 1) {
            throw new ValidationException(fieldName + " must be greater than zero");
        }
        return value;
    }

    public Integer optionalPositiveInteger(Integer value, String fieldName) {
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

    public void validateRequiredPageRange(Integer pageStart, Integer pageEnd, Integer pageCount) {
        if (pageStart == null || pageEnd == null || pageStart < 1 || pageEnd < pageStart) {
            throw new ValidationException("Page range must include pageStart and pageEnd with pageEnd >= pageStart");
        }
        if (pageCount != null && pageEnd > pageCount) {
            throw new ValidationException("Page range exceeds document page count");
        }
    }
}
