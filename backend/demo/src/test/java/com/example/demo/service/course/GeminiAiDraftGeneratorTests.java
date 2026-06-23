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
    private final InteractiveConfigService configService = new InteractiveConfigService(objectMapper, new LogicExpressionService());
    private final GeminiAiDraftGenerator generator = new GeminiAiDraftGenerator(
            new GeminiAiProperties("test-key", "gemini-test"),
            RestClient.builder(),
            objectMapper
    );

    @Test
    void parsesSubTopicInteractionConfig() throws Exception {
        @SuppressWarnings("unchecked")
        List<GeneratedSubTopicDraft> subTopics = ReflectionTestUtils.invokeMethod(generator, "parseSubTopics", """
                {"subTopics":[{"title":"Quiz topic","content":"Lesson content","mascotPrompt":"Use the variable as a lens for the experiment.","interactionType":"QUIZ","interactionPrompt":"Check understanding","interactionConfig":{"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}]}}]}
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

    @Test
    void promptGuideCoversCompleteVisualLayerAndLogicFlowContracts() {
        String guide = (String) ReflectionTestUtils.getField(GeminiAiDraftGenerator.class, "INTERACTIVE_CONFIG_GUIDE");

        assertThat(guide)
                .contains("backgroundText", "labelX", "labelY", "highlightColor", "highlightOpacity")
                .contains("button", "hotspot", "line", "x1", "y1", "x2", "y2", "qx", "qy", "flow", "arrow", "strokeWidth")
                .contains("HIGHLIGHT_ZONE", "SHOW_FEEDBACK", "sourceZoneIds", "inputs", "total", "intersection")
                .contains("CIRCUIT", "SIMPLIFY", "MATCH_OUTPUT", "allowedLaws", "lawId", "from", "to", "result", "note")
                .contains("DOUBLE_NEGATION", "DE_MORGAN", "DISTRIBUTIVE", "IMPLICATION");
    }

    @Test
    void selectionGuideEnforcesContentFirstModeMatrixAndProgression() {
        String guide = (String) ReflectionTestUtils.getField(GeminiAiDraftGenerator.class, "INTERACTIVE_SELECTION_GUIDE");

        assertThat(guide)
                .contains("Write the source-grounded subtopic content")
                .contains("Start with interactionType NONE")
                .contains("syntax reference only")
                .contains("There is no minimum interactive quota")
                .contains("GRAPH_2D: VISUALIZATION only")
                .contains("FORMULA_EXPLORER: VISUALIZATION only")
                .contains("LOGIC_FLOW SIMPLIFY: PRACTICE only")
                .contains("VISUALIZATION first", "then PRACTICE")
                .contains("programming control flow")
                .contains("Do not turn an unrelated topic into a Venn exercise");
    }

    @Test
    void parsesAndValidatesAdjacentVisualizeThenPracticeSubTopics() throws Exception {
        @SuppressWarnings("unchecked")
        List<GeneratedCourseModuleDraft> modules = ReflectionTestUtils.invokeMethod(generator, "parseCourseModules", """
                {"modules":[{"title":"Set relationships","description":"Explore before applying.","interactionType":"VISUAL_LAYER","interactionPrompt":"Learn sets in stages.","subTopics":[{"title":"Inspect the overlap","content":"Explore how sets A and B overlap spatially.","interactionType":"VISUAL_LAYER","interactionPrompt":"Inspect the two-set diagram.","interactionConfig":{"type":"VISUAL_LAYER","mode":"VISUALIZATION","title":"Inspect A and B","canvas":{"width":900,"height":520,"backgroundText":"Select a set to inspect it."},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":260,"y":130,"width":260,"height":260,"color":"#ffd333"},{"id":"zone_b","label":"B","shape":"circle","x":380,"y":130,"width":260,"height":260,"color":"#8fb3ff"}],"elements":[{"id":"choice_a","label":"Highlight A","kind":"button","x":40,"y":40,"width":150,"height":48}],"interactions":[{"triggerId":"choice_a","effect":"HIGHLIGHT_ZONE","targetZoneId":"zone_a","feedback":"This is A."}]}},{"title":"Calculate the regions","content":"Apply the totals and intersection to calculate each exact Venn region.","interactionType":"VISUAL_LAYER","interactionPrompt":"Build the diagram and enter its region values.","interactionConfig":{"type":"VISUAL_LAYER","mode":"PRACTICE","title":"Build A and B","prompt":"Arrange the circles and enter every region value.","canvas":{"width":900,"height":520,"backgroundText":"Use the supplied totals."},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":260,"y":130,"width":260,"height":260,"color":"#ffd333"},{"id":"zone_b","label":"B","shape":"circle","x":380,"y":130,"width":260,"height":260,"color":"#8fb3ff"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b"],"inputs":[{"id":"A_ONLY","label":"A total","zoneIds":["zone_a"],"value":11,"kind":"total"},{"id":"B_ONLY","label":"B total","zoneIds":["zone_b"],"value":9,"kind":"total"},{"id":"A_AND_B","label":"A intersect B","zoneIds":["zone_a","zone_b"],"value":3,"kind":"intersection"}]},"feedback":{"success":"Correct!","failure":"Check each region."}}}]}]}
                """);

        List<GeneratedSubTopicDraft> subTopics = modules.get(0).subTopics();
        assertThat(subTopics).extracting(GeneratedSubTopicDraft::title)
                .containsExactly("Inspect the overlap", "Calculate the regions");
        assertThat(subTopics.get(0).content()).isNotEqualTo(subTopics.get(1).content());
        assertThat(objectMapper.readTree(subTopics.get(0).interactionConfig()).path("mode").asText()).isEqualTo("VISUALIZATION");
        assertThat(objectMapper.readTree(subTopics.get(1).interactionConfig()).path("mode").asText()).isEqualTo("PRACTICE");
        subTopics.forEach(subTopic -> assertThat(configService.validateAndNormalize(
                subTopic.interactionType(), subTopic.interactionConfig()
        )).isNotBlank());
    }

    @Test
    void generatedFixtureUsesOnlyCurrentlySupportedModes() throws Exception {
        @SuppressWarnings("unchecked")
        List<GeneratedSubTopicDraft> subTopics = ReflectionTestUtils.invokeMethod(generator, "parseSubTopics", """
                {"subTopics":[{"title":"Plot a function","content":"Inspect the line y = 2x + 1.","interactionType":"GRAPH_2D","interactionConfig":{"type":"GRAPH_2D","mode":"VISUALIZATION","title":"Linear graph","expression":"2 * x + 1","xMin":-5,"xMax":5,"yMin":-10,"yMax":10,"sampleCount":100}},{"title":"Explore a formula","content":"Inspect how width and height affect area.","interactionType":"FORMULA_EXPLORER","interactionConfig":{"type":"FORMULA_EXPLORER","mode":"VISUALIZATION","title":"Area","formula":"width * height","variables":[{"name":"width","label":"Width","min":1,"max":10,"step":1,"initial":2},{"name":"height","label":"Height","min":1,"max":10,"step":1,"initial":3}],"precision":2}},{"title":"Inspect a circuit","content":"Explore the output of A AND B.","interactionType":"LOGIC_FLOW","interactionConfig":{"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"VISUALIZATION","title":"Explore A AND B","expression":"A & B","variables":["A","B"],"goal":"EXPLORE"}},{"title":"Match the output","content":"Apply Boolean inputs to make A AND B true.","interactionType":"LOGIC_FLOW","interactionConfig":{"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Make the output true","prompt":"Toggle both inputs.","expression":"A & B","variables":["A","B"],"goal":"TRUE","feedback":{"success":"Correct!","failure":"Try both inputs again."}}},{"title":"Simplify an implication","content":"Apply the implication law to P implies Q.","interactionType":"LOGIC_FLOW","interactionConfig":{"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Simplify P implies Q","prompt":"Apply the implication law.","start":"P -> Q","target":"!P | Q","steps":[{"law":"IMPLICATION","result":"!P | Q","note":"Rewrite the implication."}],"allowedLaws":["IMPLICATION"],"feedback":{"success":"Correct!","failure":"Try the implication law."}}},{"title":"Read the definition","content":"Review a mathematical definition that does not benefit from an activity.","interactionType":"NONE"}]}
                """);

        assertThat(subTopics).hasSize(6);
        assertThat(objectMapper.readTree(subTopics.get(0).interactionConfig()).path("mode").asText()).isEqualTo("VISUALIZATION");
        assertThat(objectMapper.readTree(subTopics.get(1).interactionConfig()).path("mode").asText()).isEqualTo("VISUALIZATION");
        assertThat(objectMapper.readTree(subTopics.get(2).interactionConfig()).path("mode").asText()).isEqualTo("VISUALIZATION");
        assertThat(objectMapper.readTree(subTopics.get(3).interactionConfig()).path("mode").asText()).isEqualTo("PRACTICE");
        assertThat(objectMapper.readTree(subTopics.get(4).interactionConfig()).path("mode").asText()).isEqualTo("PRACTICE");
        assertThat(subTopics.get(5).interactionConfig()).isNull();
        subTopics.stream()
                .filter(subTopic -> subTopic.interactionType() != InteractionType.NONE)
                .forEach(subTopic -> assertThat(configService.validateAndNormalize(
                        subTopic.interactionType(), subTopic.interactionConfig()
                )).isNotBlank());
    }
}
