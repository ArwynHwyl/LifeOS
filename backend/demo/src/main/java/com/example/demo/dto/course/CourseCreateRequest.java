package com.example.demo.dto.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CourseCreateRequest(
        @NotBlank String title,
        String description,
        String coverId,
        @Valid List<ModuleCreateRequest> modules
) {
}
