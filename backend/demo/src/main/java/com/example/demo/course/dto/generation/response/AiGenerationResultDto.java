package com.example.demo.course.dto.generation.response;

import java.util.List;

import com.example.demo.course.dto.generation.response.AiGenerationLogDto;
import com.example.demo.course.dto.management.response.SubTopicDto;

public record AiGenerationResultDto(
        AiGenerationLogDto log,
        List<SubTopicDto> generatedSubTopics
) {
}
