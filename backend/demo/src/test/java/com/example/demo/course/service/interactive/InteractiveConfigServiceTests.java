package com.example.demo.course.service.interactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.course.entity.InteractionType;
import com.example.demo.shared.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class InteractiveConfigServiceTests {

    private final InteractiveConfigService service = new InteractiveConfigService(new ObjectMapper(), new LogicExpressionService());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void noneClearsConfig() {
        assertThat(service.validateAndNormalize(InteractionType.NONE, "{\"type\":\"QUIZ\"}")).isNull();
    }

    @Test
    void acceptsValidTemplateConfigs() {
        assertThat(service.validateAndNormalize(InteractionType.GRAPH_2D, """
                {"type":"GRAPH_2D","title":"Graph","expression":"sin(x)","xMin":-10,"xMax":10,"yMin":-2,"yMax":2,"sampleCount":100}
                """)).contains("\"type\":\"GRAPH_2D\"").contains("\"mode\":\"VISUALIZATION\"");
        assertThat(service.validateAndNormalize(InteractionType.FORMULA_EXPLORER, """
                {"type":"FORMULA_EXPLORER","title":"Formula","formula":"a * b","variables":[{"name":"a","label":"A","min":1,"max":10,"step":1,"initial":2},{"name":"b","label":"B","min":1,"max":10,"step":1,"initial":3}],"precision":2}
                """)).contains("\"type\":\"FORMULA_EXPLORER\"").contains("\"mode\":\"VISUALIZATION\"");
        assertThat(service.validateAndNormalize(InteractionType.FORMULA_EXPLORER, """
                {"type":"FORMULA_EXPLORER","title":"Formula choices","formula":"a + b","variables":[{"name":"a","label":"A","min":1,"max":10,"step":1,"initial":2},{"name":"b","label":"B","min":1,"max":10,"step":1,"initial":3}],"precision":2,"formulaOptions":[{"id":"sum","label":"Sum","formula":"a + b","description":"Add the values.","steps":[{"label":"Add","expression":"a + b","explanation":"Combine both variables."}]}]}
                """)).contains("\"formulaOptions\"");
        assertThat(service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520,"backgroundText":"Venn"},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":270,"y":150,"width":220,"height":220,"color":"#ffd333","highlightColor":"#ff8f1f","highlightOpacity":0.82,"feedback":"A highlighted."}],"elements":[{"id":"choice_a","label":"Highlight A","kind":"button","x":40,"y":40,"width":150,"height":48}],"interactions":[{"triggerId":"choice_a","effect":"HIGHLIGHT_ZONE","targetZoneId":"zone_a","feedback":"A highlighted."}]}
                """)).contains("\"type\":\"VISUAL_LAYER\"").contains("\"mode\":\"VISUALIZATION\"");
        assertThat(service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":270,"y":150,"width":220,"height":220,"color":"#ffd333"},{"id":"zone_b","label":"B","shape":"circle","x":410,"y":150,"width":220,"height":220,"color":"#8fb3ff"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b"],"inputs":[{"id":"A_ONLY","label":"A","zoneIds":["zone_a"],"value":11,"kind":"total"},{"id":"B_ONLY","label":"B","zoneIds":["zone_b"],"value":9,"kind":"total"},{"id":"A_AND_B","label":"A intersect B","zoneIds":["zone_a","zone_b"],"value":3,"kind":"intersection"}]}}
                """)).contains("\"overlap\"").contains("\"value\":8.0").contains("\"value\":6.0").contains("\"value\":3.0");
        assertThat(service.validateAndNormalize(InteractionType.QUIZ, """
                {"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}]}
                """)).contains("\"type\":\"QUIZ\"").contains("\"mode\":\"PRACTICE\"");
        assertThat(service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Logic","start":"P -> Q","target":"¬P ∨ Q","allowedLaws":["IMPLICATION"],"feedback":{"success":"Correct","failure":"Try again"}}
                """)).contains("\"type\":\"LOGIC_FLOW\"").contains("\"kind\":\"SIMPLIFY\"");
    }

    @Test
    void acceptsComprehensiveGeneratedVisualAndLogicConfigs() {
        String visual = service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","mode":"VISUALIZATION","title":"Sets and flow","canvas":{"width":900,"height":520,"backgroundText":"Inspect the sets."},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":250,"y":130,"width":260,"height":260,"labelX":35,"labelY":30,"color":"#ffd333","highlightColor":"#ff8f1f","highlightOpacity":0.82,"feedback":"Set A"},{"id":"zone_b","label":"B","shape":"rectangle","x":430,"y":150,"width":260,"height":220,"labelX":65,"labelY":30,"color":"#8fb3ff","highlightColor":"#4f8cff","highlightOpacity":0.82,"feedback":"Set B"}],"elements":[{"id":"choice_a","label":"Highlight A","kind":"button","x":40,"y":40,"width":150,"height":48},{"id":"hotspot_b","label":"Inspect B","kind":"hotspot","x":720,"y":140,"width":100,"height":80},{"id":"flow_a","label":"Flow to A","kind":"line","x":200,"y":80,"width":180,"height":100,"x1":200,"y1":80,"x2":380,"y2":180,"qx":290,"qy":90,"flow":"forward","arrow":"end","color":"#3b6cb5","strokeWidth":3}],"interactions":[{"triggerId":"choice_a","effect":"HIGHLIGHT_ZONE","targetZoneId":"zone_a","feedback":"A highlighted."},{"triggerId":"hotspot_b","effect":"SHOW_FEEDBACK","feedback":"This is B."}],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b"],"inputs":[{"id":"A_ONLY","label":"A total","zoneIds":["zone_a"],"value":11,"kind":"total"},{"id":"B_ONLY","label":"B total","zoneIds":["zone_b"],"value":9,"kind":"total"},{"id":"A_AND_B","label":"A intersect B","zoneIds":["zone_a","zone_b"],"value":3,"kind":"intersection"}]}}
                """);
        String simplify = service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Simplify the implication","prompt":"Apply the implication law.","start":"P -> Q","target":"!P | Q","steps":[{"law":"IMPLICATION","lawId":"IMPLICATION","from":"P -> Q","to":"!P | Q","result":"!P | Q","note":"Rewrite the implication as a disjunction."}],"allowedLaws":["IMPLICATION"],"feedback":{"success":"Correct!","failure":"Try the implication law first."}}
                """);

        assertThat(visual)
                .contains("\"kind\":\"line\"")
                .contains("\"effect\":\"SHOW_FEEDBACK\"")
                .contains("\"values\"");
        assertThat(simplify)
                .contains("\"lawId\":\"IMPLICATION\"")
                .contains("\"from\":\"P -> Q\"")
                .contains("\"result\":\"!P | Q\"");
    }

    @Test
    void normalizesLegacyQuizConfigsToPractice() throws Exception {
        String normalized = service.validateAndNormalize(InteractionType.QUIZ, """
                {"type":"QUIZ","mode":"VISUALIZATION","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}],"hint":"Old hint","explanation":"Because."}
                """);

        JsonNode root = objectMapper.readTree(normalized);
        assertThat(root.path("mode").asText()).isEqualTo("PRACTICE");
        assertThat(root.path("prompt").asText()).isEqualTo("Pick one");
        assertThat(root.path("successCondition").path("kind").asText()).isEqualTo("QUIZ_CORRECT_OPTION");
        assertThat(root.path("feedback").path("success").asText()).isEqualTo("Correct.");
        assertThat(root.has("hint")).isFalse();
        assertThat(root.has("explanation")).isFalse();
    }

    @Test
    void exposesQuizTemplateAsPracticeOnly() {
        var quizTemplate = service.listTemplates().stream()
                .filter(template -> template.type() == InteractionType.QUIZ)
                .findFirst()
                .orElseThrow();

        assertThat(quizTemplate.defaultConfig()).containsEntry("mode", "PRACTICE");
        assertThat(quizTemplate.visualizationDefaultConfig()).containsEntry("mode", "PRACTICE");
        assertThat(quizTemplate.practiceDefaultConfig()).containsEntry("mode", "PRACTICE");
    }

    @Test
    void acceptsValidPracticeConfigs() {
        assertThat(service.validateAndNormalize(InteractionType.QUIZ, """
                {"type":"QUIZ","mode":"PRACTICE","title":"Quiz","prompt":"Pick one","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}],"successCondition":{"kind":"QUIZ_CORRECT_OPTION"},"feedback":{"success":"Correct","failure":"Try again"}}
                """)).contains("\"mode\":\"PRACTICE\"");
        assertThat(service.validateAndNormalize(InteractionType.FORMULA_EXPLORER, """
                {"type":"FORMULA_EXPLORER","mode":"PRACTICE","title":"Magnitude","prompt":"Make magnitude 5","formula":"sqrt(x^2 + y^2)","variables":[{"name":"x","label":"X","min":0,"max":10,"step":1,"initial":0},{"name":"y","label":"Y","min":0,"max":10,"step":1,"initial":0}],"precision":2,"successCondition":{"kind":"EXPRESSION_EQUALS","target":5,"tolerance":0.01},"feedback":{"success":"Correct","failure":"Not yet"}}
                """)).contains("\"mode\":\"PRACTICE\"");
        assertThat(service.validateAndNormalize(InteractionType.GRAPH_2D, """
                {"type":"GRAPH_2D","mode":"PRACTICE","title":"Line","prompt":"Set slope","expression":"m * x","controls":{"m":{"min":0,"max":5,"step":0.5,"initial":1}},"xMin":0,"xMax":5,"yMin":0,"yMax":10,"sampleCount":100,"successCondition":{"kind":"POINT_ON_GRAPH","target":{"x":2,"y":6},"tolerance":0.1},"feedback":{"success":"Correct","failure":"Not yet"}}
                """)).contains("\"mode\":\"PRACTICE\"");
    }

    @Test
    void normalizesInclusiveVisualLayerOverlapInputs() {
        String normalized = service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":260,"y":130,"width":260,"height":260,"color":"#ffd333"},{"id":"zone_b","label":"B","shape":"circle","x":380,"y":130,"width":260,"height":260,"color":"#8fb3ff"},{"id":"zone_c","label":"C","shape":"circle","x":320,"y":235,"width":260,"height":260,"color":"#8fe0aa"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b","zone_c"],"inputs":[{"id":"A_ONLY","label":"A","zoneIds":["zone_a"],"value":33,"kind":"total"},{"id":"B_ONLY","label":"B","zoneIds":["zone_b"],"value":26,"kind":"total"},{"id":"C_ONLY","label":"C","zoneIds":["zone_c"],"value":22,"kind":"total"},{"id":"A_AND_B","label":"A intersect B","zoneIds":["zone_a","zone_b"],"value":10,"kind":"intersection"},{"id":"A_AND_C","label":"A intersect C","zoneIds":["zone_a","zone_c"],"value":8,"kind":"intersection"},{"id":"B_AND_C","label":"B intersect C","zoneIds":["zone_b","zone_c"],"value":7,"kind":"intersection"},{"id":"A_AND_B_AND_C","label":"A intersect B intersect C","zoneIds":["zone_a","zone_b","zone_c"],"value":3,"kind":"intersection"}]}}
                """);

        assertThat(normalized)
                .contains("\"id\":\"A_ONLY\"")
                .contains("\"value\":18.0")
                .contains("\"id\":\"A_AND_B\"")
                .contains("\"value\":7.0")
                .contains("\"id\":\"A_AND_B_AND_C\"")
                .contains("\"value\":3.0");
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.QUIZ, "{"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("valid JSON");
    }

    @Test
    void rejectsFormulaExplorerDiagramField() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.FORMULA_EXPLORER, """
                {"type":"FORMULA_EXPLORER","title":"Formula","formula":"a + b","variables":[{"name":"a","label":"A","min":1,"max":10,"step":1,"initial":2},{"name":"b","label":"B","min":1,"max":10,"step":1,"initial":3}],"precision":2,"formulaOptions":[{"id":"sum","label":"Sum","formula":"a + b","diagram":{"kind":"VENN_2_SET","left":"a","right":"b","intersection":"a"}}]}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("formulaOptions contains unsupported field: diagram");
    }

    @Test
    void rejectsInvalidVisualLayerOverlap() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":270,"y":150,"width":220,"height":220,"color":"#ffd333"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a"],"values":[],"extra":true}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("overlap contains unsupported field");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":270,"y":150,"width":220,"height":220,"color":"#ffd333"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_missing"],"values":[]}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sourceZoneIds must reference existing zones");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":20,"y":20,"width":100,"height":100,"color":"#ffd333"},{"id":"zone_b","label":"B","shape":"circle","x":130,"y":20,"width":100,"height":100,"color":"#ffd333"},{"id":"zone_c","label":"C","shape":"circle","x":240,"y":20,"width":100,"height":100,"color":"#ffd333"},{"id":"zone_d","label":"D","shape":"circle","x":350,"y":20,"width":100,"height":100,"color":"#ffd333"},{"id":"zone_e","label":"E","shape":"circle","x":460,"y":20,"width":100,"height":100,"color":"#ffd333"},{"id":"zone_f","label":"F","shape":"circle","x":570,"y":20,"width":100,"height":100,"color":"#ffd333"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b","zone_c","zone_d","zone_e","zone_f"],"values":[]}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sourceZoneIds must contain 0 to 5 ids");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":270,"y":150,"width":220,"height":220,"color":"#ffd333"},{"id":"zone_b","label":"B","shape":"circle","x":410,"y":150,"width":220,"height":220,"color":"#8fb3ff"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b"],"values":[{"id":"A_ONLY","label":"A only","zoneIds":["zone_a"],"value":8},{"id":"A_ONLY","label":"B only","zoneIds":["zone_b"],"value":6}]}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("overlap value ids must be unique");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":270,"y":150,"width":220,"height":220,"color":"#ffd333"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a"],"values":[{"id":"A_ONLY","label":"A only","zoneIds":["zone_a"],"value":"many"}]}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("value is required");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":270,"y":150,"width":220,"height":220,"color":"#ffd333"},{"id":"zone_b","label":"B","shape":"circle","x":410,"y":150,"width":220,"height":220,"color":"#8fb3ff"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b"],"inputs":[{"id":"A_ONLY","label":"A","zoneIds":["zone_a"],"value":2,"kind":"total"},{"id":"B_ONLY","label":"B","zoneIds":["zone_b"],"value":3,"kind":"total"},{"id":"A_AND_B","label":"A intersect B","zoneIds":["zone_a","zone_b"],"value":5,"kind":"intersection"}]}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("negative exact region");
    }

    @Test
    void rejectsMismatchedType() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.QUIZ, """
                {"type":"GRAPH_2D","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}]}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must match");
    }

    @Test
    void rejectsOversizedQuizOptions() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.QUIZ, """
                {"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false},{"id":"c","label":"C","correct":false},{"id":"d","label":"D","correct":false},{"id":"e","label":"E","correct":false},{"id":"f","label":"F","correct":false},{"id":"g","label":"G","correct":false}]}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("2 to 6");
    }

    @Test
    void rejectsUnsafeExpressionIdentifier() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.GRAPH_2D, """
                {"type":"GRAPH_2D","title":"Graph","expression":"alert(x)","xMin":-10,"xMax":10,"yMin":-2,"yMax":2,"sampleCount":100}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("unsupported identifier");
    }

    @Test
    void rejectsInvalidPracticeContracts() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.QUIZ, """
                {"type":"QUIZ","mode":"PRACTICE","title":"Quiz","prompt":"Pick one","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}],"successCondition":{"kind":"POINT_ON_GRAPH"},"feedback":{"success":"Correct","failure":"Try again"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("successCondition.kind must be QUIZ_CORRECT_OPTION");

    }

    @Test
    void acceptsLogicFlowConfigsWithPrompt() {
        assertThat(service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Make it flow","prompt":"Flip the valves so the output is true.","expression":"P ∧ ¬Q","goal":"TRUE","feedback":{"success":"Correct","failure":"Try again"}}
                """)).contains("\"prompt\":\"Flip the valves so the output is true.\"");
        assertThat(service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Simplify","prompt":"Rewrite the implication.","start":"P -> Q","target":"¬P ∨ Q","feedback":{"success":"Correct","failure":"Try again"}}
                """)).contains("\"prompt\":\"Rewrite the implication.\"");
        assertThat(service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Make it flow","prompt":null,"expression":"P ∧ ¬Q","goal":"TRUE","feedback":{"success":"Correct","failure":"Try again"}}
                """)).contains("\"kind\":\"CIRCUIT\"");
    }

    @Test
    void acceptsValidLogicCircuitConfigs() {
        assertThat(service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Circuit","expression":"(A & B) | !C","variables":["A","B","C"],"goal":"MATCH_OUTPUT","feedback":{"success":"Correct","failure":"Try again"}}
                """)).contains("\"goal\":\"MATCH_OUTPUT\"");
        assertThat(service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"VISUALIZATION","title":"Explore","expression":"(P ∧ Q) ∨ ¬R","goal":"EXPLORE"}
                """)).contains("\"mode\":\"VISUALIZATION\"");
    }

    @Test
    void normalizesLowercaseGeneratedLogicVariables() throws Exception {
        String normalized = service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"VISUALIZATION","title":"Implication","expression":"p -> q","variables":["p","q"],"goal":"EXPLORE"}
                """);

        JsonNode config = objectMapper.readTree(normalized);
        assertThat(config.path("variables").toString()).isEqualTo("[\"P\",\"Q\"]");
    }

    @Test
    void rejectsInvalidLogicCircuitConfigs() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Circuit","expression":"P ∧ Q","goal":"SOMETIMES","feedback":{"success":"Correct","failure":"Try again"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("goal must be MATCH_OUTPUT, TRUE, FALSE, or EXPLORE");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Circuit","expression":"P ∧ Q","goal":"TRUE","start":"P -> Q","feedback":{"success":"Correct","failure":"Try again"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SIMPLIFY fields are not supported for CIRCUIT");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Circuit","expression":"P ∧ Q","variables":["P","Z"],"goal":"TRUE","feedback":{"success":"Correct","failure":"Try again"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("variables must match expression variables");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Circuit","expression":"P ∧ Q","goal":"TRUE"}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("feedback is required");
    }

    @Test
    void rejectsInvalidLogicSimplifyConfigs() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Simplify","start":"P -> Q","expression":"P ∧ Q","feedback":{"success":"Correct","failure":"Try again"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CIRCUIT fields are not supported for SIMPLIFY");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"VISUALIZATION","title":"Simplify","start":"P -> Q"}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("SIMPLIFY supports PRACTICE mode only");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Simplify","start":"P -> Q","steps":[{"law":"MADE_UP_LAW","result":"¬P ∨ Q"}],"feedback":{"success":"Correct","failure":"Try again"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("steps law must be a known logic law id");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"BRIDGE","mode":"PRACTICE","title":"Logic","start":"P","feedback":{"success":"Correct","failure":"Try again"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("kind must be CIRCUIT or SIMPLIFY");
    }

    @Test
    void rejectsInvalidLogicExpression() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.LOGIC_FLOW, """
                {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Logic","start":"P ->","feedback":{"success":"Correct","failure":"Try again"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("unexpectedly");
    }

    @Test
    void acceptsValidLineElements() {
        assertThat(service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {
                  "type": "VISUAL_LAYER",
                  "title": "Visual Layer with Line",
                  "canvas": {"width": 900, "height": 520},
                  "zones": [],
                  "elements": [
                    {
                      "id": "line_1",
                      "label": "My Line",
                      "kind": "line",
                      "x": 10, "y": 20, "width": 100, "height": 50,
                      "x1": 10, "y1": 20, "x2": 110, "y2": 70,
                      "qx": 60, "qy": 45,
                      "flow": "forward",
                      "arrow": "both",
                      "color": "#1a1814",
                      "strokeWidth": 3
                    }
                  ],
                  "interactions": []
                }
                """)).contains("\"kind\":\"line\"");
    }

    @Test
    void rejectsInvalidLineElements() {
        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {
                  "type": "VISUAL_LAYER",
                  "title": "Invalid Line Flow",
                  "canvas": {"width": 900, "height": 520},
                  "zones": [],
                  "elements": [
                    {
                      "id": "line_1",
                      "label": "My Line",
                      "kind": "line",
                      "x": 10, "y": 20, "width": 100, "height": 50,
                      "x1": 10, "y1": 20, "x2": 110, "y2": 70,
                      "flow": "invalid-flow"
                    }
                  ],
                  "interactions": []
                }
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("line flow must be none, forward, or backward");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {
                  "type": "VISUAL_LAYER",
                  "title": "Invalid Line Color",
                  "canvas": {"width": 900, "height": 520},
                  "zones": [],
                  "elements": [
                    {
                      "id": "line_1",
                      "label": "My Line",
                      "kind": "line",
                      "x": 10, "y": 20, "width": 100, "height": 50,
                      "x1": 10, "y1": 20, "x2": 110, "y2": 70,
                      "color": "invalid"
                    }
                  ],
                  "interactions": []
                }
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("line color must be a hex color");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {
                  "type": "VISUAL_LAYER",
                  "title": "Invalid Line Arrow",
                  "canvas": {"width": 900, "height": 520},
                  "zones": [],
                  "elements": [
                    {
                      "id": "line_1",
                      "label": "My Line",
                      "kind": "line",
                      "x": 10, "y": 20, "width": 100, "height": 50,
                      "x1": 10, "y1": 20, "x2": 110, "y2": 70,
                      "arrow": "invalid"
                    }
                  ],
                  "interactions": []
                }
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("line arrow must be none, start, end, or both");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {
                  "type": "VISUAL_LAYER",
                  "title": "Invalid Line Stroke Width",
                  "canvas": {"width": 900, "height": 520},
                  "zones": [],
                  "elements": [
                    {
                      "id": "line_1",
                      "label": "My Line",
                      "kind": "line",
                      "x": 10, "y": 20, "width": 100, "height": 50,
                      "x1": 10, "y1": 20, "x2": 110, "y2": 70,
                      "strokeWidth": 0
                    }
                  ],
                  "interactions": []
                }
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("strokeWidth");
    }

    @Test
    void acceptsMinimalLineElement() {
        assertThat(service.validateAndNormalize(InteractionType.VISUAL_LAYER, """
                {
                  "type": "VISUAL_LAYER",
                  "title": "Visual Layer with Minimal Line",
                  "canvas": {"width": 900, "height": 520},
                  "zones": [],
                  "elements": [
                    {
                      "id": "line_1",
                      "label": "My Minimal Line",
                      "kind": "line",
                      "x": 10, "y": 20, "width": 100, "height": 50,
                      "x1": 10, "y1": 20, "x2": 110, "y2": 70
                    }
                  ],
                  "interactions": []
                }
                """)).contains("\"kind\":\"line\"");
    }
}
