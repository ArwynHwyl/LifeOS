package com.example.demo.dto.course;

import java.util.List;

public record InteractiveFieldDto(
        String path,
        String label,
        String inputType,
        boolean required,
        Double min,
        Double max,
        Integer maxLength,
        List<String> options
) {
}
