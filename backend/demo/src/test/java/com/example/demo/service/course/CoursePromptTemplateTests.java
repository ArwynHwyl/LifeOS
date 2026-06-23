package com.example.demo.service.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.DocumentSource;
import org.junit.jupiter.api.Test;

class CoursePromptTemplateTests {

    private final CoursePromptTemplate template = new CoursePromptTemplate();

    @Test
    void keepsMathDirectionButMakesSourceAuthoritativeForInteractiveTopics() {
        Course course = new Course("Mathematics for Software Engineering", "Source-grounded mathematics", null);
        DocumentSource source = new DocumentSource("chapter.pdf", "Chapter", "application/pdf", 1_000L, "chapter.pdf", null, 10);

        String prompt = template.buildCourseOutline(
                course,
                source,
                1,
                5,
                "Create a focused course from this chapter.",
                "The chapter explains numerical error and floating-point approximation."
        );

        assertThat(prompt)
                .contains("Design a Math for Software Engineering course")
                .contains("Keep the selected source text and admin prompt authoritative")
                .contains("Do not introduce Set theory or Logic")
                .contains("explanation before visualization")
                .contains("visualization before guided practice");
    }
}
