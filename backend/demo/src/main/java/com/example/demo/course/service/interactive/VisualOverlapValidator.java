package com.example.demo.course.service.interactive;

import static com.example.demo.course.service.interactive.InteractiveJsonValidator.optionalText;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.rejectUnknownFields;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.requiredIdArray;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.requiredLongId;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.requiredNumber;
import static com.example.demo.course.service.interactive.InteractiveJsonValidator.requiredText;

import com.example.demo.shared.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class VisualOverlapValidator {

    private static final Set<String> OVERLAP_FIELDS = Set.of("enabled", "sourceZoneIds", "inputs", "values");
    private static final Set<String> INPUT_FIELDS = Set.of("id", "label", "zoneIds", "value", "kind");
    private static final Set<String> VALUE_FIELDS = Set.of("id", "label", "zoneIds", "value", "feedback");

    private final ObjectMapper objectMapper;

    VisualOverlapValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void validate(ObjectNode root, Set<String> zoneIds) {
        JsonNode overlap = root.get("overlap");
        if (overlap == null || overlap.isNull()) {
            return;
        }
        if (!overlap.isObject()) {
            throw new ValidationException("overlap must be an object");
        }
        ObjectNode overlapObject = (ObjectNode) overlap;
        rejectUnknownFields(overlapObject, OVERLAP_FIELDS, "overlap");
        JsonNode enabled = overlapObject.get("enabled");
        if (enabled == null || !enabled.isBoolean()) {
            throw new ValidationException("overlap.enabled must be a boolean");
        }
        List<String> sourceZoneIds = requiredIdArray(overlapObject, "sourceZoneIds", "overlap.sourceZoneIds", 0, 5);
        if (!enabled.booleanValue()) {
            if (sourceZoneIds.isEmpty()) {
                root.remove("overlap");
                return;
            }
        } else if (sourceZoneIds.isEmpty()) {
            throw new ValidationException("overlap.sourceZoneIds must contain 1 to 5 ids when enabled");
        }

        Set<String> sourceSet = new LinkedHashSet<>(sourceZoneIds);
        if (sourceSet.size() != sourceZoneIds.size()) {
            throw new ValidationException("overlap.sourceZoneIds must be unique");
        }
        for (String sourceZoneId : sourceZoneIds) {
            if (!zoneIds.contains(sourceZoneId)) {
                throw new ValidationException("overlap.sourceZoneIds must reference existing zones");
            }
        }

        JsonNode inputs = overlapObject.get("inputs");
        if (inputs != null && !inputs.isNull()) {
            validateAndNormalizeInputs(overlapObject, inputs, sourceZoneIds, sourceSet);
            return;
        }

        JsonNode values = overlapObject.get("values");
        validateLegacyValues(values, sourceZoneIds, sourceSet);
    }

    private void validateAndNormalizeInputs(
            ObjectNode overlapObject,
            JsonNode inputs,
            List<String> sourceZoneIds,
            Set<String> sourceSet
    ) {
        List<List<String>> combinations = overlapCombinations(sourceZoneIds);
        if (!inputs.isArray() || inputs.size() != combinations.size()) {
            throw new ValidationException("overlap.inputs must contain every source total and intersection");
        }
        Set<String> inputIds = new HashSet<>();
        Map<String, Double> inputValues = new java.util.HashMap<>();
        ArrayNode normalizedInputs = objectMapper.createArrayNode();
        for (JsonNode input : inputs) {
            if (!input.isObject()) {
                throw new ValidationException("overlap.inputs must contain objects");
            }
            ObjectNode inputObject = (ObjectNode) input;
            rejectUnknownFields(inputObject, INPUT_FIELDS, "overlap.inputs");
            String id = requiredLongId(inputObject, "id", "overlap input id", 240);
            if (!inputIds.add(id)) {
                throw new ValidationException("overlap input ids must be unique");
            }
            requiredText(inputObject, "label", 120);
            List<String> inputZoneIds = requiredIdArray(inputObject, "zoneIds", "overlap.inputs.zoneIds", 1, sourceZoneIds.size());
            Set<String> inputSet = new LinkedHashSet<>(inputZoneIds);
            if (inputSet.size() != inputZoneIds.size()) {
                throw new ValidationException("overlap.inputs.zoneIds must be unique");
            }
            if (!sourceSet.containsAll(inputZoneIds)) {
                throw new ValidationException("overlap.inputs.zoneIds must be subsets of sourceZoneIds");
            }
            String expectedId = overlapRegionId(inputZoneIds);
            if (!expectedId.equals(id)) {
                throw new ValidationException("overlap input id must be deterministic for its zoneIds");
            }
            String kind = requiredText(inputObject, "kind", 20);
            String expectedKind = inputZoneIds.size() == 1 ? "total" : "intersection";
            if (!expectedKind.equals(kind)) {
                throw new ValidationException("overlap input kind must match its zoneIds");
            }
            double value = requiredNumber(inputObject, "value", -1_000_000_000, 1_000_000_000);
            inputValues.put(expectedId, value);
            ObjectNode normalized = objectMapper.createObjectNode();
            normalized.put("id", expectedId);
            normalized.put("label", inputObject.get("label").asText());
            normalized.set("zoneIds", stringArray(inputZoneIds));
            normalized.put("value", value);
            normalized.put("kind", expectedKind);
            normalizedInputs.add(normalized);
        }
        for (List<String> combination : combinations) {
            if (!inputValues.containsKey(overlapRegionId(combination))) {
                throw new ValidationException("overlap.inputs must contain every source total and intersection");
            }
        }

        Map<String, ObjectNode> existingValues = new java.util.HashMap<>();
        JsonNode currentValues = overlapObject.get("values");
        if (currentValues != null && currentValues.isArray()) {
            validateLegacyValues(currentValues, sourceZoneIds, sourceSet);
            for (JsonNode value : currentValues) {
                if (value.isObject() && value.has("id")) {
                    existingValues.put(value.get("id").asText(), (ObjectNode) value);
                }
            }
        } else if (currentValues != null && !currentValues.isNull()) {
            throw new ValidationException("overlap.values must be an array of generated regions");
        }

        ArrayNode normalizedValues = objectMapper.createArrayNode();
        for (List<String> regionZoneIds : combinations) {
            String id = overlapRegionId(regionZoneIds);
            double exact = 0;
            for (List<String> candidate : combinations) {
                if (candidate.containsAll(regionZoneIds)) {
                    double inclusive = inputValues.get(overlapRegionId(candidate));
                    exact += ((candidate.size() - regionZoneIds.size()) % 2 == 0 ? 1 : -1) * inclusive;
                }
            }
            if (exact < 0) {
                throw new ValidationException("overlap inputs produce negative exact region: " + id);
            }
            ObjectNode existing = existingValues.get(id);
            ObjectNode normalized = objectMapper.createObjectNode();
            normalized.put("id", id);
            normalized.put("label", existing != null && existing.has("label") && existing.get("label").isTextual()
                    ? existing.get("label").asText()
                    : overlapRegionLabel(regionZoneIds, sourceZoneIds.size()));
            normalized.set("zoneIds", stringArray(regionZoneIds));
            normalized.put("value", exact);
            if (existing != null && existing.has("feedback") && existing.get("feedback").isTextual()) {
                normalized.put("feedback", existing.get("feedback").asText());
            }
            normalizedValues.add(normalized);
        }
        overlapObject.set("inputs", normalizedInputs);
        overlapObject.set("values", normalizedValues);
    }

    private void validateLegacyValues(JsonNode values, List<String> sourceZoneIds, Set<String> sourceSet) {
        if (values == null || !values.isArray() || values.size() > ((1 << sourceZoneIds.size()) - 1)) {
            throw new ValidationException("overlap.values must be an array of generated regions");
        }
        Set<String> valueIds = new HashSet<>();
        for (JsonNode value : values) {
            if (!value.isObject()) {
                throw new ValidationException("overlap.values must contain objects");
            }
            ObjectNode valueObject = (ObjectNode) value;
            rejectUnknownFields(valueObject, VALUE_FIELDS, "overlap.values");
            String id = requiredLongId(valueObject, "id", "overlap value id", 240);
            if (!valueIds.add(id)) {
                throw new ValidationException("overlap value ids must be unique");
            }
            requiredText(valueObject, "label", 120);
            List<String> regionZoneIds = requiredIdArray(valueObject, "zoneIds", "overlap.values.zoneIds", 1, sourceZoneIds.size());
            Set<String> regionSet = new LinkedHashSet<>(regionZoneIds);
            if (regionSet.size() != regionZoneIds.size()) {
                throw new ValidationException("overlap.values.zoneIds must be unique");
            }
            if (!sourceSet.containsAll(regionZoneIds)) {
                throw new ValidationException("overlap.values.zoneIds must be subsets of sourceZoneIds");
            }
            String expectedId = overlapRegionId(regionZoneIds);
            if (!expectedId.equals(id)) {
                throw new ValidationException("overlap value id must be deterministic for its zoneIds");
            }
            requiredNumber(valueObject, "value", -1_000_000_000, 1_000_000_000);
            optionalText(valueObject, "feedback", 500);
        }
    }

    private List<List<String>> overlapCombinations(List<String> sourceZoneIds) {
        List<String> sorted = new ArrayList<>(sourceZoneIds);
        Collections.sort(sorted);
        List<List<String>> combinations = new ArrayList<>();
        for (int mask = 1; mask < (1 << sorted.size()); mask++) {
            List<String> combination = new ArrayList<>();
            for (int index = 0; index < sorted.size(); index++) {
                if ((mask & (1 << index)) != 0) {
                    combination.add(sorted.get(index));
                }
            }
            combinations.add(combination);
        }
        return combinations;
    }

    private ArrayNode stringArray(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.forEach(array::add);
        return array;
    }

    private String overlapRegionLabel(List<String> zoneIds, int sourceCount) {
        List<String> labels = zoneIds.stream().map(this::overlapIdToken).toList();
        String base = String.join(" ∩ ", labels);
        if (zoneIds.size() == 1) {
            return base + " only";
        }
        return zoneIds.size() < sourceCount ? base + " only" : base;
    }

    private String overlapRegionId(List<String> zoneIds) {
        List<String> sorted = new ArrayList<>(zoneIds);
        Collections.sort(sorted);
        List<String> tokens = sorted.stream()
                .map(this::overlapIdToken)
                .toList();
        if (tokens.size() == 1) {
            return tokens.get(0) + "_ONLY";
        }
        return String.join("_AND_", tokens);
    }

    private String overlapIdToken(String zoneId) {
        String token = zoneId.replaceFirst("(?i)^zone[_-]?", "").replaceAll("[^A-Za-z0-9]+", "_").toUpperCase();
        token = token.replaceAll("^_+|_+$", "");
        return token.isBlank() ? zoneId.replaceAll("[^A-Za-z0-9]+", "_").toUpperCase() : token;
    }
}
