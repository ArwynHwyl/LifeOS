package com.example.demo.service.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.entity.course.InteractionType;
import com.example.demo.service.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class InteractiveConfigServiceTests {

    private final InteractiveConfigService service = new InteractiveConfigService(new ObjectMapper());

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
                {"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}],"explanation":"Because."}
                """)).contains("\"type\":\"QUIZ\"").contains("\"mode\":\"VISUALIZATION\"");
        assertThat(service.validateAndNormalize(InteractionType.THREE_JS, """
                {"type":"THREE_JS","title":"Scene","shape":"cube","color":"#4f8cff","rotationSpeed":1}
                """)).contains("\"type\":\"THREE_JS\"").contains("\"mode\":\"VISUALIZATION\"");
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
        assertThat(service.validateAndNormalize(InteractionType.THREE_JS, """
                {"type":"THREE_JS","mode":"PRACTICE","title":"Match","prompt":"Rotate it","shape":"cube","color":"#4f8cff","controls":{"rotationX":{"min":0,"max":180,"step":15,"initial":0},"rotationY":{"min":0,"max":180,"step":15,"initial":0}},"successCondition":{"kind":"TRANSFORM_MATCH","target":{"rotationX":45,"rotationY":90},"tolerance":5},"feedback":{"success":"Correct","failure":"Not yet"}}
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
                .hasMessageContaining("sourceZoneIds must contain 1 to 5 ids");

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
                {"type":"QUIZ","mode":"PRACTICE","title":"Quiz","prompt":"Pick one","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}],"feedback":{"success":"Correct","failure":"Try again"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("successCondition is required");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.THREE_JS, """
                {"type":"THREE_JS","mode":"PRACTICE","title":"Match","prompt":"Rotate it","shape":"torus","color":"#4f8cff","controls":{"rotationX":{"min":0,"max":180,"step":15,"initial":0}},"successCondition":{"kind":"TRANSFORM_MATCH","target":{"rotationX":45},"tolerance":5},"feedback":{"success":"Correct","failure":"Not yet"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("shape must be cube");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.THREE_JS, """
                {"type":"THREE_JS","mode":"PRACTICE","title":"Match","prompt":"Rotate it","shape":"cube","color":"#4f8cff","controls":{"width":{"min":0,"max":180,"step":15,"initial":0}},"successCondition":{"kind":"TRANSFORM_MATCH","target":{"width":45},"tolerance":5},"feedback":{"success":"Correct","failure":"Not yet"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("unsupported control");

        assertThatThrownBy(() -> service.validateAndNormalize(InteractionType.THREE_JS, """
                {"type":"THREE_JS","mode":"PRACTICE","title":"Match","prompt":"Rotate it","shape":"cube","color":"#4f8cff","controls":{"rotationX":{"min":0,"max":180,"step":15,"initial":0}},"successCondition":{"kind":"TRANSFORM_MATCH","target":{"rotationY":45},"tolerance":5},"feedback":{"success":"Correct","failure":"Not yet"}}
                """))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("target must reference only declared controls");
    }
}
