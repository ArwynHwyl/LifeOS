package com.example.demo.service.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.entity.course.ContentDepth;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.InteractionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MockAiDraftGeneratorTests {

    private final MockAiDraftGenerator generator = new MockAiDraftGenerator();
    private final InteractiveConfigService configService = new InteractiveConfigService(new ObjectMapper(), new LogicExpressionService());

    @Test
    void moduleDraftIncludesValidInteractiveConfigs() {
        GeneratedModuleDraft draft = generator.generateDraft(
                new CourseModule("Module", "Description", 0, ContentDepth.MEDIUM),
                "Prompt"
        );

        assertThat(draft.subTopics()).anyMatch(subTopic -> subTopic.interactionType() != InteractionType.NONE);
        draft.subTopics().stream()
                .filter(subTopic -> subTopic.interactionType() != InteractionType.NONE)
                .forEach(subTopic -> assertThat(configService.validateAndNormalize(
                        subTopic.interactionType(),
                        subTopic.interactionConfig()
                )).isNotBlank());
    }

    @Test
    void courseOutlineIncludesValidInteractiveConfigs() {
        GeneratedCourseOutlineDraft draft = generator.generateCourseOutline(
                new Course("Course", "Description", null),
                "Prompt"
        );

        assertThat(draft.modules())
                .flatExtracting(GeneratedCourseModuleDraft::subTopics)
                .anyMatch(subTopic -> subTopic.interactionType() == InteractionType.VISUAL_LAYER);
        draft.modules().stream()
                .flatMap(module -> module.subTopics().stream())
                .filter(subTopic -> subTopic.interactionType() != InteractionType.NONE)
                .forEach(subTopic -> assertThat(configService.validateAndNormalize(
                        subTopic.interactionType(),
                        subTopic.interactionConfig()
                )).isNotBlank());
    }
}
