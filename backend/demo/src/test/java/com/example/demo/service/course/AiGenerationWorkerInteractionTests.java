package com.example.demo.service.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.entity.course.InteractionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AiGenerationWorkerInteractionTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiGenerationWorker worker = new AiGenerationWorker(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new CourseInputValidator(objectMapper),
            null,
            new InteractiveConfigService(objectMapper, new LogicExpressionService())
    );

    @Test
    void normalizesValidGeneratedConfig() throws Exception {
        Object result = ReflectionTestUtils.invokeMethod(worker, "normalizeGeneratedSubTopicInteraction", new GeneratedSubTopicDraft(
                "Topic",
                "Content",
                InteractionType.QUIZ,
                "Check understanding",
                """
                        {"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}]}
                        """
        ));

        assertThat(read(result, "type")).isEqualTo(InteractionType.QUIZ);
        assertThat((String) read(result, "config")).contains("\"type\":\"QUIZ\"").contains("\"mode\":\"PRACTICE\"");
    }

    @Test
    void downgradesInvalidGeneratedConfigWithoutFailingGeneration() throws Exception {
        Object result = ReflectionTestUtils.invokeMethod(worker, "normalizeGeneratedSubTopicInteraction", new GeneratedSubTopicDraft(
                "Topic",
                "Content",
                InteractionType.QUIZ,
                "Check understanding",
                """
                        {"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true}]}
                        """
        ));

        assertThat(read(result, "type")).isEqualTo(InteractionType.NONE);
        assertThat(read(result, "config")).isNull();
        assertThat((String) read(result, "prompt")).contains("Check understanding").contains("Config needs review");
    }

    @Test
    void keepsGeneratedLogicCircuitWithLowercaseVariables() throws Exception {
        Object result = ReflectionTestUtils.invokeMethod(worker, "normalizeGeneratedSubTopicInteraction", new GeneratedSubTopicDraft(
                "Implication",
                "Explore the truth values of p implies q.",
                InteractionType.LOGIC_FLOW,
                "Toggle p and q.",
                """
                        {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"VISUALIZATION","title":"Implication","expression":"p -> q","variables":["p","q"],"goal":"EXPLORE"}
                        """
        ));

        assertThat(read(result, "type")).isEqualTo(InteractionType.LOGIC_FLOW);
        assertThat((String) read(result, "config"))
                .contains("\"variables\":[\"P\",\"Q\"]")
                .contains("\"mode\":\"VISUALIZATION\"");
    }

    private Object read(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
