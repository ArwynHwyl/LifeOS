package com.example.demo.dto.course;

import java.util.List;

public record AiGenerationResultDto(
        AiGenerationLogDto log,
        List<SubTopicDto> generatedSubTopics
) {
}
