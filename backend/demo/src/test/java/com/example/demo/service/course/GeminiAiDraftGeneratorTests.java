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
                {"subTopics":[{"title":"Quiz topic","content":"Lesson content","mascotPrompt":"Use the variable as a lens for the experiment.","interactionType":"QUIZ","interactionPrompt":"Check understanding","interactionConfig":{"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}],"explanation":"Because."}}]}
                """);

        assertThat(subTopics).hasSize(1);
        GeneratedSubTopicDraft subTopic = subTopics.get(0);
        assertThat(subTopic.mascotPrompt()).isEqualTo("Use the variable as a lens for the experiment.");
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

    @Test
    void parsesSubTopicLogicFlowConfig() throws Exception {
        @SuppressWarnings("unchecked")
        List<GeneratedSubTopicDraft> subTopics = ReflectionTestUtils.invokeMethod(generator, "parseSubTopics", """
                {"subTopics":[{"title":"Logic topic","content":"Lesson content","interactionType":"LOGIC_FLOW","interactionPrompt":"Simplify it","interactionConfig":{"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Simplify logic","start":"P -> Q","target":"!P | Q","allowedLaws":["IMPLICATION"],"feedback":{"success":"Correct!","failure":"Try again!"}}}]}
                """);

        assertThat(subTopics).hasSize(1);
        GeneratedSubTopicDraft subTopic = subTopics.get(0);
        assertThat(subTopic.interactionType()).isEqualTo(InteractionType.LOGIC_FLOW);
        assertThat(subTopic.interactionPrompt()).isEqualTo("Simplify it");
        assertThat(objectMapper.readTree(subTopic.interactionConfig()).path("kind").asText()).isEqualTo("SIMPLIFY");
        assertThat(objectMapper.readTree(subTopic.interactionConfig()).path("start").asText()).isEqualTo("P -> Q");
    }

    @Test
    void parsesSubTopicLogicCircuitConfig() throws Exception {
        @SuppressWarnings("unchecked")
        List<GeneratedSubTopicDraft> subTopics = ReflectionTestUtils.invokeMethod(generator, "parseSubTopics", """
                {"subTopics":[{"title":"Circuit topic","content":"Lesson content","interactionType":"LOGIC_FLOW","interactionPrompt":"Build gate circuit","interactionConfig":{"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Gate circuit","expression":"(A & B) | !C","variables":["A","B","C"],"goal":"MATCH_OUTPUT","feedback":{"success":"Correct!","failure":"Incorrect inputs."}}}]}
                """);

        assertThat(subTopics).hasSize(1);
        GeneratedSubTopicDraft subTopic = subTopics.get(0);
        assertThat(subTopic.interactionType()).isEqualTo(InteractionType.LOGIC_FLOW);
        assertThat(subTopic.interactionPrompt()).isEqualTo("Build gate circuit");
        assertThat(objectMapper.readTree(subTopic.interactionConfig()).path("kind").asText()).isEqualTo("CIRCUIT");
        assertThat(objectMapper.readTree(subTopic.interactionConfig()).path("expression").asText()).isEqualTo("(A & B) | !C");
    }
}
