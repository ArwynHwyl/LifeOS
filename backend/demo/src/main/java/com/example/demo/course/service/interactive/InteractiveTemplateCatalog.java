package com.example.demo.course.service.interactive;

import com.example.demo.course.dto.interactive.response.InteractiveFieldDto;
import com.example.demo.course.dto.interactive.response.InteractiveTemplateDto;
import com.example.demo.course.entity.InteractionType;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InteractiveTemplateCatalog {

    public List<InteractiveTemplateDto> listTemplates() {
        Map<String, Object> graphVisualization = graphVisualizationDefault();
        Map<String, Object> graphPractice = graphPracticeDefault();
        Map<String, Object> formulaVisualization = formulaVisualizationDefault();
        Map<String, Object> formulaPractice = formulaPracticeDefault();
        Map<String, Object> visualLayer = visualLayerDefault();
        Map<String, Object> visualLayerPractice = visualLayerPracticeDefault();
        Map<String, Object> quizPractice = quizPracticeDefault();
        Map<String, Object> logicCircuitPractice = logicCircuitPracticeDefault();
        Map<String, Object> logicSimplifyPractice = logicSimplifyPracticeDefault();
        return List.of(
                new InteractiveTemplateDto(
                        InteractionType.GRAPH_2D,
                        "2D Graph",
                        "Plot a single-variable expression across a bounded x range.",
                        graphVisualization,
                        graphVisualization,
                        graphPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("expression", "Expression", "text", true, null, null, 160, null),
                                field("xMin", "X min", "number", true, -1000.0, 1000.0, null, null),
                                field("xMax", "X max", "number", true, -1000.0, 1000.0, null, null),
                                field("yMin", "Y min", "number", true, -1000.0, 1000.0, null, null),
                                field("yMax", "Y max", "number", true, -1000.0, 1000.0, null, null),
                                field("sampleCount", "Samples", "number", true, 20.0, 500.0, null, null)
                        )
                ),
                new InteractiveTemplateDto(
                        InteractionType.FORMULA_EXPLORER,
                        "Formula Explorer",
                        "Explore a formula by adjusting bounded variables.",
                        formulaVisualization,
                        formulaVisualization,
                        formulaPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("formula", "Formula", "text", true, null, null, 160, null),
                                field("variables", "Variables", "variable-list", true, null, null, null, null),
                                field("precision", "Precision", "number", true, 0.0, 6.0, null, null)
                        )
                ),
                new InteractiveTemplateDto(
                        InteractionType.VISUAL_LAYER,
                        "Visual Layer",
                        "Build a Canva-style hotspot layer with click-to-highlight behavior.",
                        visualLayerPractice,
                        visualLayer,
                        visualLayerPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("zones", "Zones", "visual-zones", true, null, null, null, null),
                                field("elements", "Elements", "visual-elements", true, null, null, null, null),
                                field("interactions", "Interactions", "visual-interactions", true, null, null, null, null)
                        )
                ),
                new InteractiveTemplateDto(
                        InteractionType.QUIZ,
                        "Quiz",
                        "Render a local multiple-choice check with immediate feedback.",
                        quizPractice,
                        quizPractice,
                        quizPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("question", "Question", "textarea", true, null, null, 500, null),
                                field("options", "Options", "quiz-options", true, null, null, null, null)
                        )
                ),
                new InteractiveTemplateDto(
                        InteractionType.LOGIC_FLOW,
                        "Logic Flow",
                        "Grade propositional logic circuits and equivalence simplification on the server.",
                        logicCircuitPractice,
                        logicCircuitPractice,
                        logicSimplifyPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("kind", "Kind", "select", true, null, null, null, List.of("CIRCUIT", "SIMPLIFY")),
                                field("expression", "Expression", "text", false, null, null, 1000, null),
                                field("start", "Starting expression", "text", false, null, null, 1000, null),
                                field("target", "Target expression", "text", false, null, null, 1000, null)
                        )
                )
        );
    }

    private Map<String, Object> graphVisualizationDefault() {
        return Map.of(
                "type", "GRAPH_2D",
                "mode", "VISUALIZATION",
                "title", "Function graph",
                "expression", "sin(x)",
                "xMin", -6.28,
                "xMax", 6.28,
                "yMin", -2,
                "yMax", 2,
                "sampleCount", 160
        );
    }

    private Map<String, Object> graphPracticeDefault() {
        return Map.ofEntries(
                Map.entry("type", "GRAPH_2D"),
                Map.entry("mode", "PRACTICE"),
                Map.entry("title", "Match the line"),
                Map.entry("prompt", "Adjust the slope until the graph passes through the target point."),
                Map.entry("expression", "m * x"),
                Map.entry("controls", Map.of("m", Map.of("min", 0, "max", 5, "step", 0.5, "initial", 1))),
                Map.entry("xMin", 0),
                Map.entry("xMax", 5),
                Map.entry("yMin", 0),
                Map.entry("yMax", 10),
                Map.entry("sampleCount", 120),
                Map.entry("successCondition", Map.of("kind", "POINT_ON_GRAPH", "target", Map.of("x", 2, "y", 6), "tolerance", 0.15)),
                Map.entry("feedback", Map.of("success", "Correct. The curve reaches the target point.", "failure", "Not yet. Adjust the controls and compare the curve to the target."))
        );
    }

    private Map<String, Object> formulaVisualizationDefault() {
        return Map.of(
                "type", "FORMULA_EXPLORER",
                "mode", "VISUALIZATION",
                "title", "Area explorer",
                "formula", "width * height",
                "variables", List.of(
                        Map.of("name", "width", "label", "Width", "min", 1, "max", 20, "step", 1, "initial", 6),
                        Map.of("name", "height", "label", "Height", "min", 1, "max", 20, "step", 1, "initial", 4)
                ),
                "precision", 2
        );
    }

    private Map<String, Object> formulaPracticeDefault() {
        return Map.of(
                "type", "FORMULA_EXPLORER",
                "mode", "PRACTICE",
                "title", "Target magnitude",
                "prompt", "Adjust x and y until the magnitude equals 5.",
                "formula", "sqrt(x^2 + y^2)",
                "variables", List.of(
                        Map.of("name", "x", "label", "X component", "min", 0, "max", 10, "step", 1, "initial", 0),
                        Map.of("name", "y", "label", "Y component", "min", 0, "max", 10, "step", 1, "initial", 0)
                ),
                "precision", 2,
                "successCondition", Map.of("kind", "EXPRESSION_EQUALS", "target", 5, "tolerance", 0.01),
                "feedback", Map.of("success", "Correct. The magnitude is 5.", "failure", "Not yet. Look for a Pythagorean triple.")
        );
    }

    private Map<String, Object> visualLayerDefault() {
        return Map.of(
                "type", "VISUAL_LAYER",
                "mode", "VISUALIZATION",
                "title", "Visual hotspot layer",
                "canvas", Map.of(
                        "width", 900,
                        "height", 520,
                        "backgroundText", "Draw zones and connect clicks to highlights."
                ),
                "zones", List.of(
                        Map.ofEntries(
                                Map.entry("id", "zone_a"),
                                Map.entry("label", "Zone A"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 270),
                                Map.entry("y", 150),
                                Map.entry("width", 220),
                                Map.entry("height", 220),
                                Map.entry("color", "#ffd333"),
                                Map.entry("highlightColor", "#ff8f1f"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "Zone A highlighted.")
                        ),
                        Map.ofEntries(
                                Map.entry("id", "zone_b"),
                                Map.entry("label", "Zone B"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 410),
                                Map.entry("y", 150),
                                Map.entry("width", 220),
                                Map.entry("height", 220),
                                Map.entry("color", "#8fb3ff"),
                                Map.entry("highlightColor", "#4f8cff"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "Zone B highlighted.")
                        )
                ),
                "elements", List.of(
                        Map.of("id", "choice_a", "label", "Highlight A", "kind", "button", "x", 40, "y", 40, "width", 150, "height", 48)
                ),
                "interactions", List.of(
                        Map.of("triggerId", "choice_a", "effect", "HIGHLIGHT_ZONE", "targetZoneId", "zone_a", "feedback", "Zone A highlighted.")
                )
        );
    }

    private Map<String, Object> visualLayerPracticeDefault() {
        return Map.ofEntries(
                Map.entry("type", "VISUAL_LAYER"),
                Map.entry("mode", "PRACTICE"),
                Map.entry("title", "Build the Venn diagram"),
                Map.entry("prompt", "Add the required circles, arrange the overlaps, then enter the value for each visible region."),
                Map.entry("canvas", Map.of("width", 900, "height", 520, "backgroundText", "")),
                Map.entry("zones", List.of(
                        Map.ofEntries(
                                Map.entry("id", "zone_a"),
                                Map.entry("label", "A"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 250),
                                Map.entry("y", 130),
                                Map.entry("width", 260),
                                Map.entry("height", 260),
                                Map.entry("labelX", 35),
                                Map.entry("labelY", 30),
                                Map.entry("color", "#ffd333"),
                                Map.entry("highlightColor", "#ff8f1f"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "")
                        ),
                        Map.ofEntries(
                                Map.entry("id", "zone_b"),
                                Map.entry("label", "B"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 390),
                                Map.entry("y", 130),
                                Map.entry("width", 260),
                                Map.entry("height", 260),
                                Map.entry("labelX", 65),
                                Map.entry("labelY", 30),
                                Map.entry("color", "#8fb3ff"),
                                Map.entry("highlightColor", "#4f8cff"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "")
                        ),
                        Map.ofEntries(
                                Map.entry("id", "zone_c"),
                                Map.entry("label", "C"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 320),
                                Map.entry("y", 250),
                                Map.entry("width", 260),
                                Map.entry("height", 260),
                                Map.entry("labelX", 50),
                                Map.entry("labelY", 75),
                                Map.entry("color", "#8fe0aa"),
                                Map.entry("highlightColor", "#3aa66b"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "")
                        )
                )),
                Map.entry("elements", List.of()),
                Map.entry("interactions", List.of()),
                Map.entry("overlap", Map.of(
                        "enabled", true,
                        "sourceZoneIds", List.of("zone_a", "zone_b", "zone_c"),
                        "inputs", List.of(
                                Map.of("id", "A_ONLY", "label", "A", "zoneIds", List.of("zone_a"), "value", 33, "kind", "total"),
                                Map.of("id", "B_ONLY", "label", "B", "zoneIds", List.of("zone_b"), "value", 26, "kind", "total"),
                                Map.of("id", "C_ONLY", "label", "C", "zoneIds", List.of("zone_c"), "value", 22, "kind", "total"),
                                Map.of("id", "A_AND_B", "label", "A ∩ B", "zoneIds", List.of("zone_a", "zone_b"), "value", 10, "kind", "intersection"),
                                Map.of("id", "A_AND_C", "label", "A ∩ C", "zoneIds", List.of("zone_a", "zone_c"), "value", 8, "kind", "intersection"),
                                Map.of("id", "B_AND_C", "label", "B ∩ C", "zoneIds", List.of("zone_b", "zone_c"), "value", 7, "kind", "intersection"),
                                Map.of("id", "A_AND_B_AND_C", "label", "A ∩ B ∩ C", "zoneIds", List.of("zone_a", "zone_b", "zone_c"), "value", 3, "kind", "intersection")
                        ),
                        "values", List.of()
                )),
                Map.entry("feedback", Map.of(
                        "success", "Correct. The regions match the expected values.",
                        "failure", "Not yet. Check that every required overlap exists and each region value is correct."
                ))
        );
    }

    private Map<String, Object> quizPracticeDefault() {
        return Map.of(
                "type", "QUIZ",
                "mode", "PRACTICE",
                "title", "Quick check",
                "prompt", "Choose the value that satisfies the equation.",
                "question", "Which value makes 2x + 5 = 13 true?",
                "options", List.of(
                        Map.of("id", "a", "label", "x = 3", "correct", false),
                        Map.of("id", "b", "label", "x = 4", "correct", true),
                        Map.of("id", "c", "label", "x = 6", "correct", false)
                ),
                "successCondition", Map.of("kind", "QUIZ_CORRECT_OPTION"),
                "feedback", Map.of("success", "Correct.", "failure", "Not quite. Try solving for x first.")
        );
    }

    private Map<String, Object> logicCircuitPracticeDefault() {
        return Map.of(
                "type", "LOGIC_FLOW",
                "kind", "CIRCUIT",
                "mode", "PRACTICE",
                "title", "Evaluate a logic statement",
                "expression", "P ∧ ¬Q",
                "goal", "MATCH_OUTPUT",
                "feedback", Map.of("success", "Correct.", "failure", "Not yet. Recheck each truth value.")
        );
    }

    private Map<String, Object> logicSimplifyPracticeDefault() {
        return Map.of(
                "type", "LOGIC_FLOW",
                "kind", "SIMPLIFY",
                "mode", "PRACTICE",
                "title", "Simplify an implication",
                "start", "P -> Q",
                "target", "¬P ∨ Q",
                "allowedLaws", List.of("IMPLICATION", "DOUBLE_NEGATION", "DE_MORGAN"),
                "feedback", Map.of("success", "Correct. The expression is logically equivalent.", "failure", "Not yet. Check the implication law.")
        );
    }

    private InteractiveFieldDto field(
            String path,
            String label,
            String inputType,
            boolean required,
            Double min,
            Double max,
            Integer maxLength,
            List<String> options
    ) {
        return new InteractiveFieldDto(path, label, inputType, required, min, max, maxLength, options);
    }
}
