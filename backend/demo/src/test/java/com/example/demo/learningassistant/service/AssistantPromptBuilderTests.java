package com.example.demo.learningassistant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.learningassistant.entity.AssistantMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssistantPromptBuilderTests {
    private final AssistantPromptBuilder builder = new AssistantPromptBuilder();

    @Test
    void explainPromptContainsPersonaLessonAndSelection() {
        AssistantStreamContext context = context(AssistantMode.EXPLAIN, "Please explain", "empty set");

        String prompt = builder.build(context, List.of());

        assertThat(prompt)
                .contains("calm and concise learning companion")
                .contains("Always answer in clear English")
                .contains("Course: Discrete Math")
                .contains("Current section: Sets")
                .contains("Selected lesson text: empty set")
                .contains("End with exactly one short comprehension-check question")
                .contains("Learner request: Please explain");
    }

    @Test
    void hintPromptForbidsFinalAnswerAndAllowsOnlyOneHint() {
        String prompt = builder.build(context(AssistantMode.HINT, "Give me the answer", null), List.of());

        assertThat(prompt)
                .contains("Give exactly one next hint")
                .contains("Do not reveal the final answer, even when asked")
                .contains("Ask at most one question");
    }

    private AssistantStreamContext context(AssistantMode mode, String message, String selectedText) {
        return new AssistantStreamContext(1L, 2L, 3L, mode, message, selectedText,
                "Discrete Math", "Foundations", "Sets", "A set is a collection. The empty set has no members.",
                "Let's work through it one step at a time.", null, null);
    }
}
