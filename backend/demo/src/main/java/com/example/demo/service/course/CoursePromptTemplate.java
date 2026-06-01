package com.example.demo.service.course;

import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.DocumentSource;
import org.springframework.stereotype.Component;

@Component
class CoursePromptTemplate {

    String build(
            CourseModule module,
            DocumentSource documentSource,
            Integer pageStart,
            Integer pageEnd,
            String requirements,
            String sourceText
    ) {
        return """
                Generate draft learning content for the LifeOS course module.

                Course module: %s
                Module description: %s
                Content depth: %s
                Source document: %s
                Selected pages: %d-%d
                Admin requirements: %s

                Extracted source text:
                %s

                Return concise structured sub-topics with a title and learner-facing content.
                Treat the output as a draft. Do not claim teacher validation or publication.
                """.formatted(
                module.getTitle(),
                nullToEmpty(module.getDescription()),
                module.getContentDepth(),
                documentSource.getDisplayName(),
                pageStart,
                pageEnd,
                nullToEmpty(requirements),
                nullToEmpty(sourceText)
        ).trim();
    }

    String buildCourseOutline(
            Course course,
            DocumentSource documentSource,
            Integer pageStart,
            Integer pageEnd,
            String adminPrompt,
            String sourceText
    ) {
        return """
                Generate a complete draft course outline for LifeOS.

                Course title: %s
                Course description: %s
                Source document: %s
                Selected pages: %d-%d

                Admin prompt:
                %s

                Default course direction when the admin prompt is incomplete:
                Design a Math for Software Engineering course. Emphasize mathematical concepts that help learners reason about programs, algorithms, graphics, data, and AI systems. Suggest interactive or visual learning ideas only when they improve learning, and generate valid interactionConfig objects for the supported generated types: QUIZ, GRAPH_2D, FORMULA_EXPLORER, and VISUAL_LAYER.

                Extracted source text:
                %s

                Treat the output as an editable draft course outline. Do not claim teacher validation or publication.
                """.formatted(
                course.getTitle(),
                nullToEmpty(course.getDescription()),
                documentSource.getDisplayName(),
                pageStart,
                pageEnd,
                nullToEmpty(adminPrompt),
                nullToEmpty(sourceText)
        ).trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
