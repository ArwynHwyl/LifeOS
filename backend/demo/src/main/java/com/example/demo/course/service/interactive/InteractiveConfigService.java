package com.example.demo.course.service.interactive;

import static com.example.demo.course.service.interactive.InteractiveJsonValidator.optionalNumber;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.optionalText;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.rejectUnknownFields;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.requiredId;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.requiredInteger;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.requiredNumber;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.requiredText;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.validateVisualBounds;

import com.example.demo.course.dto.interactive.response.InteractiveTemplateDto;
import com.example.demo.course.entity.InteractionType;
import com.example.demo.shared.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InteractiveConfigService {

    private static final int MAX_CONFIG_LENGTH = 50_000;
    private static final Set<String> PRACTICE_FIELDS = Set.of("prompt", "successCondition", "feedback");
    private static final Set<String> GRAPH_FIELDS = Set.of(
            "type", "mode", "title", "expression", "xMin", "xMax", "yMin", "yMax", "sampleCount", "controls", "prompt", "successCondition", "feedback"
    );
    private static final Set<String> FORMULA_FIELDS = Set.of(
            "type", "mode", "title", "formula", "variables", "precision", "prompt", "successCondition", "feedback", "formulaOptions"
    );
    private static final Set<String> VARIABLE_FIELDS = Set.of(
            "name", "label", "min", "max", "step", "initial"
    );
    private static final Set<String> FORMULA_OPTION_FIELDS = Set.of(
            "id", "label", "formula", "description", "steps"
    );
    private static final Set<String> FORMULA_STEP_FIELDS = Set.of(
            "label", "expression", "explanation"
    );
    private static final Set<String> VISUAL_LAYER_FIELDS = Set.of("type", "mode", "title", "canvas", "zones", "elements", "interactions", "overlap", "prompt", "feedback");
    private static final Set<String> VISUAL_CANVAS_FIELDS = Set.of("width", "height", "backgroundText");
    private static final Set<String> VISUAL_ZONE_FIELDS = Set.of("id", "label", "shape", "x", "y", "width", "height", "labelX", "labelY", "color", "highlightColor", "highlightOpacity", "feedback");
    private static final Set<String> VISUAL_ELEMENT_FIELDS = Set.of(
            "id", "label", "kind", "x", "y", "width", "height",
            "x1", "y1", "x2", "y2", "qx", "qy", "flow", "arrow", "color", "strokeWidth"
    );
    private static final Set<String> VISUAL_INTERACTION_FIELDS = Set.of("triggerId", "effect", "targetZoneId", "feedback");
    private static final Set<String> QUIZ_FIELDS = Set.of(
            "type", "mode", "title", "question", "options", "prompt", "successCondition", "feedback"
    );
    private static final Set<String> QUIZ_OPTION_FIELDS = Set.of("id", "label", "correct");
    private static final Set<String> LOGIC_FIELDS = Set.of(
            "type", "kind", "mode", "title", "prompt", "start", "target", "steps", "allowedLaws", "expression", "variables", "goal", "feedback"
    );
    private static final Set<String> LOGIC_STEP_FIELDS = Set.of("law", "lawId", "result", "from", "to", "note");
    private static final Set<String> LOGIC_LAW_IDS = Set.of(
            "DOUBLE_NEGATION", "DE_MORGAN", "DISTRIBUTIVE", "IDENTITY", "DOMINATION", "IDEMPOTENT",
            "COMPLEMENT", "ABSORPTION", "COMMUTATIVE", "ASSOCIATIVE", "IMPLICATION"
    );
    private static final Set<String> CONTROL_FIELDS = Set.of("min", "max", "step", "initial");
    private static final Set<String> SUCCESS_CONDITION_FIELDS = Set.of("kind", "target", "tolerance", "correctOptionId");
    private static final Set<String> FEEDBACK_FIELDS = Set.of("success", "failure");

    private final ObjectMapper objectMapper;
    private final LogicExpressionService logicExpressionService;
    private final InteractiveTemplateCatalog templateCatalog;
    private final VisualOverlapValidator visualOverlapValidator;

    public InteractiveConfigService(
            ObjectMapper objectMapper,
            LogicExpressionService logicExpressionService,
            InteractiveTemplateCatalog templateCatalog
    ) {
        this.objectMapper = objectMapper;
        this.logicExpressionService = logicExpressionService;
        this.templateCatalog = templateCatalog;
        this.visualOverlapValidator = new VisualOverlapValidator(objectMapper);
    }

    public String validateAndNormalize(InteractionType interactionType, String interactionConfig) {
        InteractionType type = interactionType == null ? InteractionType.NONE : interactionType;
        if (type == InteractionType.NONE) {
            return null;
        }
        if (type == InteractionType.OTHER) {
            return optionalJson(interactionConfig);
        }
        if (!isTemplate(type)) {
            throw new ValidationException("Unsupported interactionType: " + type);
        }
        if (interactionConfig == null || interactionConfig.isBlank()) {
            throw new ValidationException("interactionConfig is required for " + type);
        }
        if (interactionConfig.trim().length() > MAX_CONFIG_LENGTH) {
            throw new ValidationException("interactionConfig must be " + MAX_CONFIG_LENGTH + " characters or fewer");
        }
        ObjectNode root = parseObject(interactionConfig);
        requireType(root, type);
        boolean practice = normalizeAndValidateMode(root, type);
        switch (type) {
            case GRAPH_2D -> validateGraph(root, practice);
            case FORMULA_EXPLORER -> validateFormula(root, practice);
            case VISUAL_LAYER -> validateVisualLayer(root, practice);
            case LOGIC_FLOW -> validateLogicFlow(root, practice);
            case QUIZ -> validateQuiz(root, practice);
            default -> throw new ValidationException("Unsupported interactionType: " + type);
        }
        return stringify(root);
    }

    public List<InteractiveTemplateDto> listTemplates() {
        return templateCatalog.listTemplates();
    }

    private boolean isTemplate(InteractionType type) {
        return type == InteractionType.GRAPH_2D
                || type == InteractionType.FORMULA_EXPLORER
                || type == InteractionType.VISUAL_LAYER
                || type == InteractionType.LOGIC_FLOW
                || type == InteractionType.QUIZ;
    }

    private String optionalJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.trim().length() > MAX_CONFIG_LENGTH) {
            throw new ValidationException("interactionConfig must be " + MAX_CONFIG_LENGTH + " characters or fewer");
        }
        try {
            objectMapper.readTree(value);
            return value.trim();
        } catch (JsonProcessingException ex) {
            throw new ValidationException("interactionConfig must be valid JSON");
        }
    }

    private ObjectNode parseObject(String value) {
        try {
            JsonNode root = objectMapper.readTree(value);
            if (!root.isObject()) {
                throw new ValidationException("interactionConfig must be a JSON object");
            }
            return (ObjectNode) root;
        } catch (JsonProcessingException ex) {
            throw new ValidationException("interactionConfig must be valid JSON");
        }
    }

    private void requireType(ObjectNode root, InteractionType type) {
        String configType = requiredText(root, "type", 30);
        if (!type.name().equals(configType)) {
            throw new ValidationException("interactionConfig.type must match interactionType");
        }
    }

    private boolean normalizeAndValidateMode(ObjectNode root, InteractionType type) {
        JsonNode mode = root.get("mode");
        if (mode == null || mode.isNull()) {
            if (type == InteractionType.QUIZ) {
                normalizeQuizPracticeFields(root);
                return true;
            }
            root.put("mode", "VISUALIZATION");
            return false;
        }
        if (!mode.isTextual()) {
            throw new ValidationException("mode must be VISUALIZATION or PRACTICE");
        }
        String value = mode.asText().trim();
        if (!Set.of("VISUALIZATION", "PRACTICE").contains(value)) {
            throw new ValidationException("mode must be VISUALIZATION or PRACTICE");
        }
        if (type == InteractionType.QUIZ) {
            root.put("mode", "PRACTICE");
            normalizeQuizPracticeFields(root);
            return true;
        }
        root.put("mode", value);
        return "PRACTICE".equals(value);
    }

    private void normalizeQuizPracticeFields(ObjectNode root) {
        root.put("mode", "PRACTICE");
        if (!root.hasNonNull("prompt")) {
            String question = root.path("question").isTextual() ? root.path("question").asText() : "Choose the correct answer.";
            root.put("prompt", question);
        }
        JsonNode successCondition = root.get("successCondition");
        if (successCondition == null || successCondition.isNull()) {
            ObjectNode condition = root.putObject("successCondition");
            condition.put("kind", "QUIZ_CORRECT_OPTION");
        } else if (successCondition.isObject()) {
            ObjectNode condition = (ObjectNode) successCondition;
            JsonNode legacyType = condition.get("type");
            if (!condition.hasNonNull("kind") && legacyType != null && legacyType.isTextual()) {
                condition.put("kind", legacyType.asText());
            }
            condition.remove("type");
        }
        JsonNode feedback = root.get("feedback");
        if (feedback == null || feedback.isNull()) {
            ObjectNode feedbackObject = root.putObject("feedback");
            feedbackObject.put("success", "Correct.");
            feedbackObject.put("failure", "Not quite. Try again.");
        } else if (feedback.isObject()) {
            ObjectNode feedbackObject = (ObjectNode) feedback;
            if (!feedbackObject.hasNonNull("success")) {
                feedbackObject.put("success", "Correct.");
            }
            if (!feedbackObject.hasNonNull("failure")) {
                feedbackObject.put("failure", "Not quite. Try again.");
            }
        }
    }

    private void validateGraph(ObjectNode root, boolean practice) {
        rejectUnknownFields(root, GRAPH_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        Set<String> variables = new java.util.HashSet<>(Set.of("x"));
        Map<String, double[]> controls = practice ? validateControls(root, null, 1, 6) : Map.of();
        variables.addAll(controls.keySet());
        String expression = requiredText(root, "expression", 160);
        validateExpression(expression, variables);
        double xMin = requiredNumber(root, "xMin", -1000, 1000);
        double xMax = requiredNumber(root, "xMax", -1000, 1000);
        double yMin = requiredNumber(root, "yMin", -1000, 1000);
        double yMax = requiredNumber(root, "yMax", -1000, 1000);
        if (xMax <= xMin) {
            throw new ValidationException("xMax must be greater than xMin");
        }
        if (yMax <= yMin) {
            throw new ValidationException("yMax must be greater than yMin");
        }
        requiredInteger(root, "sampleCount", 20, 500);
        if (practice) {
            validatePracticeCommon(root, "POINT_ON_GRAPH");
            validatePointOnGraphCondition((ObjectNode) root.get("successCondition"), xMin, xMax, yMin, yMax);
        } else {
            rejectVisualizationPracticeFields(root);
        }
    }

    private void validateFormula(ObjectNode root, boolean practice) {
        rejectUnknownFields(root, FORMULA_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        String formula = requiredText(root, "formula", 160);
        JsonNode variables = root.get("variables");
        if (variables == null || !variables.isArray() || variables.size() < 1 || variables.size() > 10) {
            throw new ValidationException("variables must contain 1 to 10 items");
        }
        Set<String> names = new java.util.HashSet<>();
        for (JsonNode variable : variables) {
            if (!variable.isObject()) {
                throw new ValidationException("variables must contain objects");
            }
            ObjectNode variableObject = (ObjectNode) variable;
            rejectUnknownFields(variableObject, VARIABLE_FIELDS, "variables");
            String name = requiredText(variableObject, "name", 30);
            if (!name.matches("[A-Za-z][A-Za-z0-9_]{0,29}")) {
                throw new ValidationException("variable name must start with a letter and contain only letters, numbers, or underscores");
            }
            if (!names.add(name)) {
                throw new ValidationException("variable names must be unique");
            }
            requiredText(variableObject, "label", 80);
            double min = requiredNumber(variableObject, "min", -1000, 1000);
            double max = requiredNumber(variableObject, "max", -1000, 1000);
            if (max <= min) {
                throw new ValidationException("variable max must be greater than min");
            }
            double step = requiredNumber(variableObject, "step", 0.0001, 1000);
            double initial = requiredNumber(variableObject, "initial", min, max);
            if (step > max - min) {
                throw new ValidationException("variable step must fit inside the variable range");
            }
        }
        validateExpression(formula, names);
        validateFormulaOptions(root, names);
        requiredInteger(root, "precision", 0, 6);
        if (practice) {
            validatePracticeCommon(root, "EXPRESSION_EQUALS");
            validateExpressionEqualsCondition((ObjectNode) root.get("successCondition"));
        } else {
            rejectVisualizationPracticeFields(root);
        }
    }

    private void validateFormulaOptions(ObjectNode root, Set<String> names) {
        JsonNode options = root.get("formulaOptions");
        if (options == null || options.isNull()) {
            return;
        }
        if (!options.isArray() || options.size() < 1 || options.size() > 10) {
            throw new ValidationException("formulaOptions must contain 1 to 10 items");
        }
        Set<String> ids = new java.util.HashSet<>();
        for (JsonNode option : options) {
            if (!option.isObject()) {
                throw new ValidationException("formulaOptions must contain objects");
            }
            ObjectNode optionObject = (ObjectNode) option;
            rejectUnknownFields(optionObject, FORMULA_OPTION_FIELDS, "formulaOptions");
            String id = requiredText(optionObject, "id", 40);
            if (!id.matches("[A-Za-z][A-Za-z0-9_-]{0,39}")) {
                throw new ValidationException("formula option id must start with a letter and contain only letters, numbers, underscores, or dashes");
            }
            if (!ids.add(id)) {
                throw new ValidationException("formula option ids must be unique");
            }
            requiredText(optionObject, "label", 120);
            validateExpression(requiredText(optionObject, "formula", 160), names);
            optionalText(optionObject, "description", 500);
            validateFormulaSteps(optionObject, names);
        }
    }

    private void validateFormulaSteps(ObjectNode optionObject, Set<String> names) {
        JsonNode steps = optionObject.get("steps");
        if (steps == null || steps.isNull()) {
            return;
        }
        if (!steps.isArray() || steps.size() < 1 || steps.size() > 8) {
            throw new ValidationException("formula steps must contain 1 to 8 items");
        }
        for (JsonNode step : steps) {
            if (!step.isObject()) {
                throw new ValidationException("formula steps must contain objects");
            }
            ObjectNode stepObject = (ObjectNode) step;
            rejectUnknownFields(stepObject, FORMULA_STEP_FIELDS, "formulaOptions.steps");
            requiredText(stepObject, "label", 120);
            validateExpression(requiredText(stepObject, "expression", 160), names);
            optionalText(stepObject, "explanation", 500);
        }
    }

    private void validateVisualLayer(ObjectNode root, boolean practice) {
        rejectUnknownFields(root, VISUAL_LAYER_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        JsonNode canvas = root.get("canvas");
        if (canvas == null || !canvas.isObject()) {
            throw new ValidationException("canvas is required for VISUAL_LAYER");
        }
        ObjectNode canvasObject = (ObjectNode) canvas;
        rejectUnknownFields(canvasObject, VISUAL_CANVAS_FIELDS, "canvas");
        double canvasWidth = requiredNumber(canvasObject, "width", 320, 2400);
        double canvasHeight = requiredNumber(canvasObject, "height", 240, 1800);
        optionalText(canvasObject, "backgroundText", 500);

        Set<String> zoneIds = validateVisualZones(root, canvasWidth, canvasHeight);
        Set<String> elementIds = validateVisualElements(root, canvasWidth, canvasHeight);
        validateVisualInteractions(root, zoneIds, elementIds);
        visualOverlapValidator.validate(root, zoneIds);
        if (practice) {
            validateVisualPracticeCommon(root);
        } else {
            rejectVisualizationPracticeFields(root);
        }
    }

    private Set<String> validateVisualZones(ObjectNode root, double canvasWidth, double canvasHeight) {
        JsonNode zones = root.get("zones");
        if (zones == null || !zones.isArray() || zones.size() > 40) {
            throw new ValidationException("zones must be an array with 0 to 40 items");
        }
        Set<String> ids = new java.util.HashSet<>();
        for (JsonNode zone : zones) {
            if (!zone.isObject()) {
                throw new ValidationException("zones must contain objects");
            }
            ObjectNode zoneObject = (ObjectNode) zone;
            rejectUnknownFields(zoneObject, VISUAL_ZONE_FIELDS, "zones");
            String id = requiredId(zoneObject, "id", "zone id");
            if (!ids.add(id)) {
                throw new ValidationException("zone ids must be unique");
            }
            requiredText(zoneObject, "label", 80);
            String shape = requiredText(zoneObject, "shape", 20);
            if (!Set.of("rectangle", "circle").contains(shape)) {
                throw new ValidationException("zone shape must be rectangle or circle");
            }
            validateVisualBounds(zoneObject, canvasWidth, canvasHeight);
            optionalNumber(zoneObject, "labelX", 0, 100);
            optionalNumber(zoneObject, "labelY", 0, 100);
            String color = requiredText(zoneObject, "color", 7);
            if (!color.matches("#[0-9A-Fa-f]{6}")) {
                throw new ValidationException("zone color must be a hex color");
            }
            String highlightColor = optionalText(zoneObject, "highlightColor", 7);
            if (highlightColor != null && !highlightColor.matches("#[0-9A-Fa-f]{6}")) {
                throw new ValidationException("zone highlightColor must be a hex color");
            }
            optionalNumber(zoneObject, "highlightOpacity", 0, 1);
            optionalText(zoneObject, "feedback", 500);
        }
        return ids;
    }

    private Set<String> validateVisualElements(ObjectNode root, double canvasWidth, double canvasHeight) {
        JsonNode elements = root.get("elements");
        if (elements == null || !elements.isArray() || elements.size() > 40) {
            throw new ValidationException("elements must be an array with 0 to 40 items");
        }
        Set<String> ids = new java.util.HashSet<>();
        for (JsonNode element : elements) {
            if (!element.isObject()) {
                throw new ValidationException("elements must contain objects");
            }
            ObjectNode elementObject = (ObjectNode) element;
            rejectUnknownFields(elementObject, VISUAL_ELEMENT_FIELDS, "elements");
            String id = requiredId(elementObject, "id", "element id");
            if (!ids.add(id)) {
                throw new ValidationException("element ids must be unique");
            }
            requiredText(elementObject, "label", 120);
            String kind = requiredText(elementObject, "kind", 20);
            if (!Set.of("button", "hotspot", "line").contains(kind)) {
                throw new ValidationException("element kind must be button, hotspot, or line");
            }
            if ("line".equals(kind)) {
                requiredNumber(elementObject, "x", 0, canvasWidth);
                requiredNumber(elementObject, "y", 0, canvasHeight);
                requiredNumber(elementObject, "width", 0, canvasWidth);
                requiredNumber(elementObject, "height", 0, canvasHeight);
                requiredNumber(elementObject, "x1", 0, canvasWidth);
                requiredNumber(elementObject, "y1", 0, canvasHeight);
                requiredNumber(elementObject, "x2", 0, canvasWidth);
                requiredNumber(elementObject, "y2", 0, canvasHeight);
                optionalNumber(elementObject, "qx", 0, canvasWidth);
                optionalNumber(elementObject, "qy", 0, canvasHeight);
                String flow = optionalText(elementObject, "flow", 20);
                if (flow != null && !Set.of("none", "forward", "backward").contains(flow)) {
                    throw new ValidationException("line flow must be none, forward, or backward");
                }
                String arrow = optionalText(elementObject, "arrow", 10);
                if (arrow != null && !Set.of("none", "start", "end", "both").contains(arrow)) {
                    throw new ValidationException("line arrow must be none, start, end, or both");
                }
                String color = optionalText(elementObject, "color", 7);
                if (color != null && !color.matches("#[0-9A-Fa-f]{6}")) {
                    throw new ValidationException("line color must be a hex color");
                }
                optionalNumber(elementObject, "strokeWidth", 1, 20);
            } else {
                validateVisualBounds(elementObject, canvasWidth, canvasHeight);
            }
        }
        return ids;
    }

    private void validateVisualInteractions(ObjectNode root, Set<String> zoneIds, Set<String> elementIds) {
        JsonNode interactions = root.get("interactions");
        if (interactions == null || !interactions.isArray() || interactions.size() > 80) {
            throw new ValidationException("interactions must be an array with 0 to 80 items");
        }
        for (JsonNode interaction : interactions) {
            if (!interaction.isObject()) {
                throw new ValidationException("interactions must contain objects");
            }
            ObjectNode interactionObject = (ObjectNode) interaction;
            rejectUnknownFields(interactionObject, VISUAL_INTERACTION_FIELDS, "interactions");
            String triggerId = requiredId(interactionObject, "triggerId", "triggerId");
            if (!elementIds.contains(triggerId) && !zoneIds.contains(triggerId)) {
                throw new ValidationException("interaction triggerId must reference a zone or element");
            }
            String effect = requiredText(interactionObject, "effect", 40);
            if (!Set.of("HIGHLIGHT_ZONE", "SHOW_FEEDBACK").contains(effect)) {
                throw new ValidationException("interaction effect is not supported");
            }
            if ("HIGHLIGHT_ZONE".equals(effect)) {
                String targetZoneId = requiredId(interactionObject, "targetZoneId", "targetZoneId");
                if (!zoneIds.contains(targetZoneId)) {
                    throw new ValidationException("targetZoneId must reference a zone");
                }
            }
            optionalText(interactionObject, "feedback", 500);
        }
    }

    private ArrayNode stringArray(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.forEach(array::add);
        return array;
    }

    private void validateQuiz(ObjectNode root, boolean practice) {
        root.remove(List.of("hint", "explanation"));
        rejectUnknownFields(root, QUIZ_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        requiredText(root, "question", 500);
        JsonNode options = root.get("options");
        if (options == null || !options.isArray() || options.size() < 2 || options.size() > 6) {
            throw new ValidationException("options must contain 2 to 6 items");
        }
        int correctCount = 0;
        Set<String> ids = new java.util.HashSet<>();
        for (JsonNode option : options) {
            if (!option.isObject()) {
                throw new ValidationException("options must contain objects");
            }
            ObjectNode optionObject = (ObjectNode) option;
            rejectUnknownFields(optionObject, QUIZ_OPTION_FIELDS, "options");
            String id = requiredText(optionObject, "id", 24);
            if (!ids.add(id)) {
                throw new ValidationException("option ids must be unique");
            }
            requiredText(optionObject, "label", 250);
            JsonNode correct = optionObject.get("correct");
            if (correct == null || !correct.isBoolean()) {
                throw new ValidationException("option correct must be true or false");
            }
            if (correct.booleanValue()) {
                correctCount++;
            }
        }
        if (correctCount != 1) {
            throw new ValidationException("quiz must contain exactly one correct option");
        }
        if (practice) {
            validatePracticeCommon(root, "QUIZ_CORRECT_OPTION");
            JsonNode correctOptionId = root.get("successCondition").get("correctOptionId");
            if (correctOptionId != null && (!correctOptionId.isTextual() || !ids.contains(correctOptionId.asText()))) {
                throw new ValidationException("correctOptionId must match a quiz option id");
            }
        } else {
            rejectVisualizationPracticeFields(root);
        }
    }

    private void validateLogicFlow(ObjectNode root, boolean practice) {
        rejectUnknownFields(root, LOGIC_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        optionalText(root, "prompt", 500);
        String kind = requiredText(root, "kind", 20);
        if (!Set.of("CIRCUIT", "SIMPLIFY").contains(kind)) {
            throw new ValidationException("kind must be CIRCUIT or SIMPLIFY");
        }
        if ("CIRCUIT".equals(kind)) {
            validateLogicCircuit(root, practice);
        } else {
            validateLogicSimplify(root, practice);
        }
    }

    private void validateLogicCircuit(ObjectNode root, boolean practice) {
        String expression = requiredText(root, "expression", 1000);
        LogicExpressionService.Node ast = logicExpressionService.parse(expression);
        Set<String> expressionVariables = logicExpressionService.variables(ast);
        if (expressionVariables.size() > 8) {
            throw new ValidationException("logic expression can contain at most 8 variables");
        }
        JsonNode variables = root.get("variables");
        if (variables != null && !variables.isNull()) {
            if (!variables.isArray() || variables.size() > 8) {
                throw new ValidationException("variables must contain 0 to 8 items");
            }
            Set<String> normalizedVariables = new LinkedHashSet<>();
            for (JsonNode variable : variables) {
                if (!variable.isTextual()) {
                    throw new ValidationException("variables must match expression variables");
                }
                String normalizedVariable = variable.asText().trim().toUpperCase(java.util.Locale.ROOT);
                if (!expressionVariables.contains(normalizedVariable)) {
                    throw new ValidationException("variables must match expression variables");
                }
                normalizedVariables.add(normalizedVariable);
            }
            root.set("variables", stringArray(new ArrayList<>(normalizedVariables)));
        }
        String goal = optionalText(root, "goal", 40);
        if (goal != null && !Set.of("MATCH_OUTPUT", "TRUE", "FALSE", "EXPLORE").contains(goal)) {
            throw new ValidationException("goal must be MATCH_OUTPUT, TRUE, FALSE, or EXPLORE");
        }
        if (root.has("start") || root.has("target") || root.has("steps") || root.has("allowedLaws")) {
            throw new ValidationException("SIMPLIFY fields are not supported for CIRCUIT");
        }
        if (practice) {
            validateLogicFeedback(root);
        } else {
            rejectLogicPracticeFields(root);
        }
    }

    private void validateLogicSimplify(ObjectNode root, boolean practice) {
        String start = requiredText(root, "start", 1000);
        LogicExpressionService.Node startAst = logicExpressionService.parse(start);
        if (logicExpressionService.variables(startAst).size() > 8) {
            throw new ValidationException("logic expression can contain at most 8 variables");
        }
        String target = optionalText(root, "target", 1000);
        if (target != null) {
            LogicExpressionService.Node targetAst = logicExpressionService.parse(target);
            if (logicExpressionService.variables(targetAst).size() > 8) {
                throw new ValidationException("logic expression can contain at most 8 variables");
            }
        }
        validateLogicSteps(root);
        validateAllowedLogicLaws(root);
        if (root.has("expression") || root.has("variables") || root.has("goal")) {
            throw new ValidationException("CIRCUIT fields are not supported for SIMPLIFY");
        }
        if (practice) {
            validateLogicFeedback(root);
        } else {
            throw new ValidationException("SIMPLIFY supports PRACTICE mode only");
        }
    }

    private void validateLogicFeedback(ObjectNode root) {
        JsonNode feedback = root.get("feedback");
        if (feedback == null || !feedback.isObject()) {
            throw new ValidationException("feedback is required for LOGIC_FLOW practice interactions");
        }
        ObjectNode feedbackObject = (ObjectNode) feedback;
        rejectUnknownFields(feedbackObject, FEEDBACK_FIELDS, "feedback");
        requiredText(feedbackObject, "success", 500);
        requiredText(feedbackObject, "failure", 500);
    }

    private void rejectLogicPracticeFields(ObjectNode root) {
        root.remove("feedback");
    }

    private void validateLogicSteps(ObjectNode root) {
        JsonNode steps = root.get("steps");
        if (steps == null || steps.isNull()) {
            return;
        }
        if (!steps.isArray() || steps.size() > 20) {
            throw new ValidationException("steps must contain 0 to 20 items");
        }
        for (JsonNode step : steps) {
            if (!step.isObject()) {
                throw new ValidationException("steps must contain objects");
            }
            ObjectNode stepObject = (ObjectNode) step;
            rejectUnknownFields(stepObject, LOGIC_STEP_FIELDS, "steps");
            String law = optionalText(stepObject, "law", 80);
            String lawId = optionalText(stepObject, "lawId", 80);
            if (law != null && !LOGIC_LAW_IDS.contains(law)) {
                throw new ValidationException("steps law must be a known logic law id");
            }
            if (lawId != null && !LOGIC_LAW_IDS.contains(lawId)) {
                throw new ValidationException("steps lawId must be a known logic law id");
            }
            validateOptionalLogicExpression(stepObject, "result");
            validateOptionalLogicExpression(stepObject, "from");
            validateOptionalLogicExpression(stepObject, "to");
            optionalText(stepObject, "note", 500);
        }
    }

    private void validateAllowedLogicLaws(ObjectNode root) {
        JsonNode allowedLaws = root.get("allowedLaws");
        if (allowedLaws == null || allowedLaws.isNull()) {
            return;
        }
        if (!allowedLaws.isArray() || allowedLaws.size() > LOGIC_LAW_IDS.size()) {
            throw new ValidationException("allowedLaws must contain known logic law ids");
        }
        Set<String> ids = new HashSet<>();
        for (JsonNode law : allowedLaws) {
            if (!law.isTextual() || !LOGIC_LAW_IDS.contains(law.asText()) || !ids.add(law.asText())) {
                throw new ValidationException("allowedLaws must contain unique known logic law ids");
            }
        }
    }

    private void validateOptionalLogicExpression(ObjectNode node, String fieldName) {
        String expression = optionalText(node, fieldName, 1000);
        if (expression != null) {
            logicExpressionService.parse(expression);
        }
    }

    private void validatePracticeCommon(ObjectNode root, String expectedKind) {
        requiredText(root, "prompt", 500);
        JsonNode feedback = root.get("feedback");
        if (feedback == null || !feedback.isObject()) {
            throw new ValidationException("feedback is required for practice interactions");
        }
        ObjectNode feedbackObject = (ObjectNode) feedback;
        rejectUnknownFields(feedbackObject, FEEDBACK_FIELDS, "feedback");
        requiredText(feedbackObject, "success", 500);
        requiredText(feedbackObject, "failure", 500);

        JsonNode condition = root.get("successCondition");
        if (condition == null || !condition.isObject()) {
            throw new ValidationException("successCondition is required for practice interactions");
        }
        ObjectNode conditionObject = (ObjectNode) condition;
        rejectUnknownFields(conditionObject, SUCCESS_CONDITION_FIELDS, "successCondition");
        String kind = requiredText(conditionObject, "kind", 40);
        if (!expectedKind.equals(kind)) {
            throw new ValidationException("successCondition.kind must be " + expectedKind);
        }
    }

    private void validateVisualPracticeCommon(ObjectNode root) {
        requiredText(root, "prompt", 500);
        JsonNode feedback = root.get("feedback");
        if (feedback == null || !feedback.isObject()) {
            throw new ValidationException("feedback is required for practice interactions");
        }
        ObjectNode feedbackObject = (ObjectNode) feedback;
        rejectUnknownFields(feedbackObject, FEEDBACK_FIELDS, "feedback");
        requiredText(feedbackObject, "success", 500);
        requiredText(feedbackObject, "failure", 500);
        JsonNode overlap = root.get("overlap");
        if (overlap == null || !overlap.isObject() || !overlap.path("enabled").asBoolean(false)) {
            throw new ValidationException("overlap is required for visual layer practice");
        }
        JsonNode sourceZoneIds = overlap.get("sourceZoneIds");
        if (sourceZoneIds == null || !sourceZoneIds.isArray() || sourceZoneIds.size() < 2 || sourceZoneIds.size() > 3) {
            throw new ValidationException("visual layer practice requires 2 to 3 overlap source zones");
        }
    }

    private void rejectVisualizationPracticeFields(ObjectNode root) {
        for (String field : PRACTICE_FIELDS) {
            root.remove(field);
        }
        root.remove("controls");
    }

    private Map<String, double[]> validateControls(ObjectNode root, Set<String> allowedNames, int minCount, int maxCount) {
        JsonNode controls = root.get("controls");
        if (controls == null || !controls.isObject()) {
            throw new ValidationException("controls is required for this practice interaction");
        }
        ObjectNode controlsObject = (ObjectNode) controls;
        int count = 0;
        Map<String, double[]> ranges = new java.util.HashMap<>();
        Iterator<String> names = controlsObject.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            count++;
            if (!name.matches("[A-Za-z][A-Za-z0-9_]{0,29}")) {
                throw new ValidationException("control name must start with a letter and contain only letters, numbers, or underscores");
            }
            if (allowedNames != null && !allowedNames.contains(name)) {
                throw new ValidationException("unsupported control: " + name);
            }
            JsonNode control = controlsObject.get(name);
            if (!control.isObject()) {
                throw new ValidationException("controls must contain objects");
            }
            ObjectNode controlObject = (ObjectNode) control;
            rejectUnknownFields(controlObject, CONTROL_FIELDS, "controls");
            double min = requiredNumber(controlObject, "min", -1000, 1000);
            double max = requiredNumber(controlObject, "max", -1000, 1000);
            if (max <= min) {
                throw new ValidationException("control max must be greater than min");
            }
            double step = requiredNumber(controlObject, "step", 0.0001, 1000);
            if (step > max - min) {
                throw new ValidationException("control step must fit inside the control range");
            }
            requiredNumber(controlObject, "initial", min, max);
            ranges.put(name, new double[] { min, max });
        }
        if (count < minCount || count > maxCount) {
            throw new ValidationException("controls must contain " + minCount + " to " + maxCount + " items");
        }
        return ranges;
    }

    private void validateExpressionEqualsCondition(ObjectNode condition) {
        requiredNumber(condition, "target", -1_000_000, 1_000_000);
        optionalNumber(condition, "tolerance", 0, 1_000_000);
    }

    private void validatePointOnGraphCondition(ObjectNode condition, double xMin, double xMax, double yMin, double yMax) {
        JsonNode target = condition.get("target");
        if (target == null || !target.isObject()) {
            throw new ValidationException("successCondition.target is required");
        }
        ObjectNode targetObject = (ObjectNode) target;
        rejectUnknownFields(targetObject, Set.of("x", "y"), "successCondition.target");
        requiredNumber(targetObject, "x", xMin, xMax);
        requiredNumber(targetObject, "y", yMin, yMax);
        optionalNumber(condition, "tolerance", 0, 1000);
    }

    private void validateExpression(String expression, Set<String> allowedVariables) {
        if (!expression.matches("[A-Za-z0-9_+\\-*/^().,\\s]+")) {
            throw new ValidationException("expression contains unsupported characters");
        }
        for (String token : expression.split("[^A-Za-z0-9_]+")) {
            if (token.isBlank() || token.matches("\\d+")) {
                continue;
            }
            if (allowedVariables.contains(token) || Set.of("sin", "cos", "tan", "sqrt", "abs", "log", "ln", "exp", "min", "max", "pow", "pi", "e").contains(token)) {
                continue;
            }
            throw new ValidationException("expression contains unsupported identifier: " + token);
        }
    }

    private String stringify(ObjectNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("interactionConfig must be valid JSON");
        }
    }
}
