package com.example.demo.service.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.config.GeminiAiProperties;
import com.example.demo.entity.course.InteractionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class GeminiAiDraftGeneratorTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeminiAiDraftGenerator generator = new GeminiAiDraftGenerator(
            new GeminiAiProperties("test-key", "gemini-test"),
            RestClient.builder(),
            objectMapper
    );

    @Test
    void parsesSubTopicInteractionConfig() throws Exception {
        @SuppressWarnings("unchecked")
        List<GeneratedSubTopicDraft> subTopics = ReflectionTestUtils.invokeMethod(generator, "parseSubTopics", """
                {"subTopics":[{"title":"Quiz topic","content":"Lesson content","interactionType":"QUIZ","interactionPrompt":"Check understanding","interactionConfig":{"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}],"explanation":"Because."}}]}
                """);

        assertThat(subTopics).hasSize(1);
        GeneratedSubTopicDraft subTopic = subTopics.get(0);
        assertThat(subTopic.interactionType()).isEqualTo(InteractionType.QUIZ);
        assertThat(subTopic.interactionPrompt()).isEqualTo("Check understanding");
        assertThat(objectMapper.readTree(subTopic.interactionConfig()).path("type").asText()).isEqualTo("QUIZ");
    }

    @Test
    void parsesCourseOutlineSubTopicInteractionConfig() throws Exception {
        @SuppressWarnings("unchecked")
        List<GeneratedCourseModuleDraft> modules = ReflectionTestUtils.invokeMethod(generator, "parseCourseModules", """
                {"modules":[{"title":"Module","description":"Description","interactionType":"NONE","interactionPrompt":null,"subTopics":[{"title":"Graph topic","content":"Lesson content","interactionType":"GRAPH_2D","interactionPrompt":"Plot it","interactionConfig":{"type":"GRAPH_2D","title":"Graph","expression":"2 * x","xMin":-5,"xMax":5,"yMin":-10,"yMax":10,"sampleCount":100}}]}]}
                """);

        GeneratedSubTopicDraft subTopic = modules.get(0).subTopics().get(0);
        assertThat(subTopic.interactionType()).isEqualTo(InteractionType.GRAPH_2D);
        assertThat(objectMapper.readTree(subTopic.interactionConfig()).path("expression").asText()).isEqualTo("2 * x");
    }
}
