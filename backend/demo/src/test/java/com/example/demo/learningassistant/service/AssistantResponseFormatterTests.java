package com.example.demo.learningassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.learningassistant.entity.AssistantMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AssistantResponseFormatterTests {
    private final AssistantResponseFormatter formatter = new AssistantResponseFormatter(new ObjectMapper());

    @Test
    void explainResponseHasAtMostOneExampleAndExactlyOneQuestion() {
        String answer = formatter.format(AssistantMode.EXPLAIN, """
                {"teachingPoints":["A proposition has one truth value."],"examples":["P can be true."],"followUpQuestions":["What truth value can P have?"]}
                """);

        assertThat(answer).isEqualTo("A proposition has one truth value.\n\nExample: P can be true.\n\nWhat truth value can P have?");
        assertThat(answer.chars().filter(character -> character == '?').count()).isEqualTo(1);
    }

    @Test
    void hintResponseRejectsAnExample() {
        assertThatThrownBy(() -> formatter.format(AssistantMode.HINT, """
                {"teachingPoints":["Start with the left branch."],"examples":["Set P to true."],"followUpQuestions":["Which gate should you inspect first?"]}
                """))
                .isInstanceOf(AssistantResponseFormatter.InvalidAssistantResponseException.class)
                .hasMessageContaining("cannot include an example");
    }

    @Test
    void responseRejectsQuestionsOutsideTheFollowUpField() {
        assertThatThrownBy(() -> formatter.format(AssistantMode.EXPLAIN, """
                {"teachingPoints":["What is a proposition? It is a statement."],"examples":[],"followUpQuestions":["Can you give one?"]}
                """))
                .isInstanceOf(AssistantResponseFormatter.InvalidAssistantResponseException.class)
                .hasMessageContaining("Only followUpQuestion");
    }

    @Test
    void responseRejectsMultipleFollowUpQuestions() {
        assertThatThrownBy(() -> formatter.format(AssistantMode.EXPLAIN, """
                {"teachingPoints":["A proposition is a statement."],"examples":[],"followUpQuestions":["Is this clear? Can you give one?"]}
                """))
                .isInstanceOf(AssistantResponseFormatter.InvalidAssistantResponseException.class)
                .hasMessageContaining("exactly one question");
    }

    @Test
    void responseRejectsMoreThanOneTeachingPoint() {
        assertThatThrownBy(() -> formatter.format(AssistantMode.HINT, """
                {"teachingPoints":["Inspect the left branch.","Then set P to true."],"examples":[],"followUpQuestions":["Which branch will you inspect?"]}
                """))
                .isInstanceOf(AssistantResponseFormatter.InvalidAssistantResponseException.class)
                .hasMessageContaining("exactly one text item");
    }
}
