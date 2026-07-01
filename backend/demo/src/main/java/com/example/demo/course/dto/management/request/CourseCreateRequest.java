package com.example.demo.course.dto.management.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

import com.example.demo.course.dto.management.request.ModuleCreateRequest;

public record CourseCreateRequest(
        @NotBlank String title,
        String description,
        String coverId,
        @Valid List<ModuleCreateRequest> modules
) {
}
