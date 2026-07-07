package com.example.demo.course.dto.interactive.response;

import java.util.List;
import java.util.Map;

import com.example.demo.course.entity.InteractionType;

public record InteractiveTemplateDto(
        InteractionType type,
        String label,
        String description,
        Map<String, Object> defaultConfig,
        Map<String, Object> visualizationDefaultConfig,
        Map<String, Object> practiceDefaultConfig,
        List<InteractiveFieldDto> editableFields
) {
}
