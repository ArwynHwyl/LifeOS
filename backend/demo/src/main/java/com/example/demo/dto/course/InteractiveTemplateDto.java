package com.example.demo.dto.course;

import com.example.demo.entity.course.InteractionType;
import java.util.List;
import java.util.Map;

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
