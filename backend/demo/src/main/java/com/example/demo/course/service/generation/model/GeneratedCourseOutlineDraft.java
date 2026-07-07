package com.example.demo.course.service.generation.model;

import java.util.List;

public record GeneratedCourseOutlineDraft(
        String rawResponse,
        List<GeneratedCourseModuleDraft> modules
) {
}
