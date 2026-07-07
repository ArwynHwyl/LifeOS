package com.example.demo.course.service.learner;

import com.example.demo.course.mapper.CourseDtoMapper;
import com.example.demo.course.service.management.CourseInputValidator;
import com.example.demo.course.service.interactive.LogicExpressionService;
import com.example.demo.course.service.interactive.MathExpressionService;

import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseStatus;
import com.example.demo.course.entity.InteractionType;
import com.example.demo.course.entity.InteractiveProgressStatus;
import com.example.demo.course.entity.LearnerInteractiveProgress;
import com.example.demo.course.entity.SubTopic;
import com.example.demo.course.repository.CourseRepository;
import com.example.demo.course.repository.LearnerInteractiveProgressRepository;
import com.example.demo.course.dto.interactive.request.InteractiveAttemptRequest;
import com.example.demo.course.dto.interactive.response.InteractiveAttemptResponse;
import com.example.demo.course.dto.interactive.response.PublishedCourseDetailDto;
import com.example.demo.course.dto.interactive.response.PublishedCourseSummaryDto;
import com.example.demo.course.dto.interactive.response.InteractiveProgressDto;
import com.example.demo.course.dto.interactive.request.InteractiveProgressUpdateRequest;
import com.example.demo.course.dto.interactive.request.LogicAttemptRequest;
import com.example.demo.course.dto.interactive.response.LogicAttemptResponse;
import com.example.demo.course.dto.interactive.response.LogicStepSubmissionDto;
import com.example.demo.gamification.service.GamificationService;
import com.example.demo.shared.exception.ApiException;
import com.example.demo.user.entity.User;
import com.example.demo.shared.exception.InvalidWorkflowStateException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearnerCourseService {

    private static final int SUBTOPIC_MASTERY_BASE_EXP = 10;

    private final CourseRepository courseRepository;
    private final LearnerInteractiveProgressRepository progressRepository;
    private final EntityManager entityManager;
    private final CourseDtoMapper mapper;
    private final CourseInputValidator validator;
    private final ObjectMapper objectMapper;
    private final LogicExpressionService logicExpressionService;
    private final MathExpressionService mathExpressionService;
    private final GamificationService gamificationService;

    public LearnerCourseService(
            CourseRepository courseRepository,
            LearnerInteractiveProgressRepository progressRepository,
            EntityManager entityManager,
            CourseDtoMapper mapper,
            CourseInputValidator validator,
            ObjectMapper objectMapper,
            LogicExpressionService logicExpressionService,
            MathExpressionService mathExpressionService,
            GamificationService gamificationService
    ) {
        this.courseRepository = courseRepository;
        this.progressRepository = progressRepository;
        this.entityManager = entityManager;
        this.mapper = mapper;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.logicExpressionService = logicExpressionService;
        this.mathExpressionService = mathExpressionService;
        this.gamificationService = gamificationService;
    }

    @Transactional(readOnly = true)
    public List<PublishedCourseSummaryDto> listPublishedCourses() {
        return mapper.toPublishedSummaryDtos(courseRepository.findByStatusOrderByUpdatedAtDesc(CourseStatus.PUBLISHED));
    }

    @Transactional(readOnly = true)
    public PublishedCourseDetailDto getPublishedCourse(UUID userId, Long courseId) {
        Course course = findPublishedCourse(courseId);
        List<SubTopic> allTopics = course.getModules().stream()
                .flatMap(module -> module.getSubTopics().stream())
                .toList();
        return mapper.toPublishedDetailDto(course, progressBySubTopicId(userId, allTopics));
    }

    @Transactional
    public InteractiveProgressDto updateInteractiveProgress(
            UUID userId,
            Long courseId,
            Long subTopicId,
            InteractiveProgressUpdateRequest request
    ) {
        Long requiredCourseId = validator.requiredId(courseId, "courseId");
        Long requiredSubTopicId = validator.requiredId(subTopicId, "subTopicId");
        InteractiveProgressStatus requestedStatus = request.status();
        if (requestedStatus == null || requestedStatus == InteractiveProgressStatus.NOT_STARTED) {
            throw new ValidationException("status must be TRIED or MASTERED");
        }

        Course course = findPublishedCourseById(requiredCourseId, courseId);
        SubTopic subTopic = findSubTopicInPublishedCourse(course, requiredSubTopicId);
        if (requestedStatus == InteractiveProgressStatus.MASTERED && isServerGradable(subTopic.getInteractionType()) && !isVisualizationMode(subTopic)) {
            throw new InvalidWorkflowStateException(subTopic.getInteractionType() + " mastery must be recorded through interactive-attempts");
        }

        LearnerInteractiveProgress progress = findOrCreateProgress(userId, course, subTopic);
        return toProgressDto(recordAttemptAndAward(userId, progress, requestedStatus));
    }

    @Transactional
    public LogicAttemptResponse submitLogicAttempt(
            UUID userId,
            Long courseId,
            Long subTopicId,
            LogicAttemptRequest request
    ) {
        Long requiredCourseId = validator.requiredId(courseId, "courseId");
        Long requiredSubTopicId = validator.requiredId(subTopicId, "subTopicId");
        if (request == null) {
            throw new ValidationException("logic attempt request is required");
        }

        Course course = findPublishedCourseById(requiredCourseId, courseId);
        SubTopic subTopic = findInteractiveSubTopicInPublishedCourse(course, requiredSubTopicId);
        if (subTopic.getInteractionType() != InteractionType.LOGIC_FLOW) {
            throw new InvalidWorkflowStateException("Subtopic is not a LOGIC_FLOW interactive");
        }

        JsonNode config = parseLogicConfig(subTopic);
        String kind = requiredKind(request.kind());
        String configKind = requiredConfigText(config, "kind");
        if (!kind.equals(configKind)) {
            throw new ValidationException("kind must match interactionConfig.kind");
        }

        GradeResult grade = "SIMPLIFY".equals(kind)
                ? gradeSimplify(config, request)
                : gradeCircuit(config, request);
        InteractiveProgressStatus nextStatus = grade.correct()
                ? InteractiveProgressStatus.MASTERED
                : InteractiveProgressStatus.TRIED;
        LearnerInteractiveProgress progress = findOrCreateProgress(userId, course, subTopic);
        LearnerInteractiveProgress saved = recordAttemptAndAward(userId, progress, nextStatus);
        return new LogicAttemptResponse(
                requiredSubTopicId,
                kind,
                grade.correct(),
                saved.getStatus(),
                saved.getAttemptCount(),
                saved.getMasteredAt(),
                saved.getUpdatedAt(),
                grade.feedback(),
                grade.details()
        );
    }

    @Transactional
    public InteractiveAttemptResponse submitInteractiveAttempt(
            UUID userId,
            Long courseId,
            Long subTopicId,
            InteractiveAttemptRequest request
    ) {
        Long requiredCourseId = validator.requiredId(courseId, "courseId");
        Long requiredSubTopicId = validator.requiredId(subTopicId, "subTopicId");
        if (request == null) {
            throw new ValidationException("interactive attempt request is required");
        }
        Course course = findPublishedCourseById(requiredCourseId, courseId);
        SubTopic subTopic = findInteractiveSubTopicInPublishedCourse(course, requiredSubTopicId);
        if (subTopic.getInteractionType() == InteractionType.LOGIC_FLOW) {
            LogicAttemptResponse logic = submitLogicAttempt(userId, courseId, requiredSubTopicId, toLogicAttempt(request, subTopic));
            return new InteractiveAttemptResponse(
                    logic.subTopicId(),
                    InteractionType.LOGIC_FLOW,
                    logic.correct(),
                    logic.status(),
                    logic.attemptCount(),
                    logic.masteredAt(),
                    logic.updatedAt(),
                    logic.feedback(),
                    logic.details()
            );
        }
        GradeResult grade = gradeInteractive(subTopic, request);
        InteractiveProgressStatus nextStatus = grade.correct()
                ? InteractiveProgressStatus.MASTERED
                : InteractiveProgressStatus.TRIED;
        LearnerInteractiveProgress progress = findOrCreateProgress(userId, course, subTopic);
        LearnerInteractiveProgress saved = recordAttemptAndAward(userId, progress, nextStatus);
        return new InteractiveAttemptResponse(
                requiredSubTopicId,
                subTopic.getInteractionType(),
                grade.correct(),
                saved.getStatus(),
                saved.getAttemptCount(),
                saved.getMasteredAt(),
                saved.getUpdatedAt(),
                grade.feedback(),
                grade.details()
        );
    }

    private Course findPublishedCourse(Long courseId) {
        Long requiredCourseId = validator.requiredId(courseId, "courseId");
        return findPublishedCourseById(requiredCourseId, courseId);
    }

    private Course findPublishedCourseById(Long requiredCourseId, Long requestedCourseId) {
        return courseRepository.findById(requiredCourseId)
                .filter(foundCourse -> foundCourse.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published course not found: " + requestedCourseId));
    }

    private SubTopic findSubTopicInPublishedCourse(Course course, Long subTopicId) {
        return course.getModules().stream()
                .flatMap(module -> module.getSubTopics().stream())
                .filter(item -> item.getId().equals(subTopicId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Subtopic not found in published course: " + subTopicId));
    }

    private SubTopic findInteractiveSubTopicInPublishedCourse(Course course, Long subTopicId) {
        return interactiveSubTopics(course).stream()
                .filter(item -> item.getId().equals(subTopicId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Interactive subtopic not found in published course: " + subTopicId));
    }

    private LearnerInteractiveProgress findOrCreateProgress(UUID userId, Course course, SubTopic subTopic) {
        return progressRepository.findByUserUserIdAndSubTopicId(userId, subTopic.getId())
                .orElseGet(() -> new LearnerInteractiveProgress(
                        entityManager.getReference(User.class, userId),
                        course,
                        subTopic
                ));
    }

    private LearnerInteractiveProgress recordAttemptAndAward(
            UUID userId, LearnerInteractiveProgress progress, InteractiveProgressStatus nextStatus) {
        boolean wasAlreadyMastered = progress.getStatus() == InteractiveProgressStatus.MASTERED;
        progress.recordAttempt(nextStatus);
        LearnerInteractiveProgress saved = progressRepository.save(progress);
        if (!wasAlreadyMastered && saved.getStatus() == InteractiveProgressStatus.MASTERED) {
            gamificationService.recordSubtopicCompletion(userId, SUBTOPIC_MASTERY_BASE_EXP);
        }
        return saved;
    }

    private List<SubTopic> interactiveSubTopics(Course course) {
        return course.getModules().stream()
                .flatMap(module -> module.getSubTopics().stream())
                .filter(subTopic -> subTopic.getInteractionType() != InteractionType.NONE)
                .toList();
    }

    private boolean isServerGradable(InteractionType interactionType) {
        return interactionType == InteractionType.QUIZ
                || interactionType == InteractionType.GRAPH_2D
                || interactionType == InteractionType.FORMULA_EXPLORER
                || interactionType == InteractionType.VISUAL_LAYER
                || interactionType == InteractionType.LOGIC_FLOW;
    }

    private Map<Long, InteractiveProgressDto> progressBySubTopicId(UUID userId, List<SubTopic> interactiveSubTopics) {
        List<Long> subTopicIds = interactiveSubTopics.stream().map(SubTopic::getId).toList();
        if (subTopicIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, LearnerInteractiveProgress> savedProgress = progressRepository
                .findByUserUserIdAndSubTopicIdIn(userId, subTopicIds)
                .stream()
                .collect(Collectors.toMap(progress -> progress.getSubTopic().getId(), Function.identity()));
        return interactiveSubTopics.stream()
                .collect(Collectors.toMap(
                        SubTopic::getId,
                        subTopic -> {
                            LearnerInteractiveProgress saved = savedProgress.get(subTopic.getId());
                            return saved == null ? notStartedProgressDto(subTopic.getId()) : toProgressDto(saved);
                        }
                ));
    }

    private InteractiveProgressDto notStartedProgressDto(Long subTopicId) {
        return new InteractiveProgressDto(subTopicId, InteractiveProgressStatus.NOT_STARTED, 0, null, null);
    }

    private InteractiveProgressDto toProgressDto(LearnerInteractiveProgress progress) {
        return new InteractiveProgressDto(
                progress.getSubTopic().getId(),
                progress.getStatus(),
                progress.getAttemptCount(),
                progress.getMasteredAt(),
                progress.getUpdatedAt()
        );
    }

    private JsonNode parseLogicConfig(SubTopic subTopic) {
        try {
            JsonNode config = objectMapper.readTree(subTopic.getInteractionConfig());
            if (!config.isObject()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "LOGIC_FLOW config cannot be graded");
            }
            return config;
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "LOGIC_FLOW config cannot be graded");
        }
    }

    private GradeResult gradeInteractive(SubTopic subTopic, InteractiveAttemptRequest request) {
        JsonNode config = parseInteractionConfig(subTopic);
        return switch (subTopic.getInteractionType()) {
            case QUIZ -> gradeQuiz(config, request);
            case FORMULA_EXPLORER -> gradeFormula(config, request);
            case GRAPH_2D -> gradeGraph(config, request);
            case VISUAL_LAYER -> gradeVisualLayer(config, request);
            default -> throw new InvalidWorkflowStateException("Subtopic does not support server-graded attempts");
        };
    }

    private JsonNode parseInteractionConfig(SubTopic subTopic) {
        try {
            JsonNode config = objectMapper.readTree(subTopic.getInteractionConfig());
            if (!config.isObject()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "interactive config cannot be graded");
            }
            return config;
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "interactive config cannot be graded");
        }
    }

    private GradeResult gradeQuiz(JsonNode config, InteractiveAttemptRequest request) {
        String selected = textAnswer(request.answer());
        JsonNode options = config.get("options");
        if (options == null || !options.isArray()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "QUIZ config cannot be graded");
        }
        boolean correct = false;
        boolean found = false;
        for (JsonNode option : options) {
            if (selected.equals(option.path("id").asText())) {
                found = true;
                correct = option.path("correct").asBoolean(false);
                break;
            }
        }
        if (!found) {
            throw new ValidationException("answer must match a quiz option id");
        }
        return new GradeResult(correct, feedback(config, correct), Map.of("selectedOptionId", selected));
    }

    private GradeResult gradeFormula(JsonNode config, InteractiveAttemptRequest request) {
        Map<String, Double> values = request.values();
        if (values == null || values.isEmpty()) {
            throw new ValidationException("values are required");
        }
        JsonNode variables = config.get("variables");
        if (variables == null || !variables.isArray()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "FORMULA_EXPLORER config cannot be graded");
        }
        Set<String> expectedNames = new java.util.HashSet<>();
        for (JsonNode variable : variables) {
            expectedNames.add(variable.path("name").asText());
        }
        if (!values.keySet().equals(expectedNames)) {
            throw new ValidationException("values must exactly match formula variables");
        }
        JsonNode condition = config.get("successCondition");
        if (condition == null || !"EXPRESSION_EQUALS".equals(condition.path("kind").asText())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "FORMULA_EXPLORER config cannot be graded");
        }
        double result = mathExpressionService.evaluate(requiredConfigText(config, "formula"), values);
        double target = condition.path("target").asDouble();
        double tolerance = condition.has("tolerance") ? condition.path("tolerance").asDouble() : 0;
        boolean correct = Math.abs(result - target) <= tolerance;
        return new GradeResult(correct, feedback(config, correct), Map.of("result", result, "target", target));
    }

    private GradeResult gradeGraph(JsonNode config, InteractiveAttemptRequest request) {
        Map<String, Double> values = request.values() == null ? Map.of() : request.values();
        JsonNode condition = config.get("successCondition");
        if (condition == null || !"POINT_ON_GRAPH".equals(condition.path("kind").asText())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GRAPH_2D config cannot be graded");
        }
        JsonNode target = condition.get("target");
        if (target == null || !target.isObject()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "GRAPH_2D config cannot be graded");
        }
        java.util.HashMap<String, Double> variables = new java.util.HashMap<>(values);
        double x = target.path("x").asDouble();
        double y = target.path("y").asDouble();
        variables.put("x", x);
        double result = mathExpressionService.evaluate(requiredConfigText(config, "expression"), variables);
        double tolerance = condition.has("tolerance") ? condition.path("tolerance").asDouble() : 0;
        boolean correct = Math.abs(result - y) <= tolerance;
        return new GradeResult(correct, feedback(config, correct), Map.of("result", result, "target", y));
    }

    private GradeResult gradeVisualLayer(JsonNode config, InteractiveAttemptRequest request) {
        Map<String, Double> answers = request.regionAnswers();
        if (answers == null || answers.isEmpty()) {
            throw new ValidationException("regionAnswers are required");
        }
        JsonNode values = config.path("overlap").get("values");
        if (values == null || !values.isArray()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VISUAL_LAYER config cannot be graded");
        }
        java.util.HashMap<String, Double> expected = new java.util.HashMap<>();
        for (JsonNode value : values) {
            double number = value.path("value").asDouble();
            if (number != 0) {
                expected.put(value.path("id").asText(), number);
            }
        }
        boolean correct = answers.keySet().containsAll(expected.keySet()) && expected.entrySet().stream()
                .allMatch(entry -> Math.abs((answers.get(entry.getKey()) == null ? Double.NaN : answers.get(entry.getKey())) - entry.getValue()) < 0.0000001);
        return new GradeResult(correct, feedback(config, correct), Map.of("expectedRegionCount", expected.size()));
    }

    private LogicAttemptRequest toLogicAttempt(InteractiveAttemptRequest request, SubTopic subTopic) {
        JsonNode config = parseLogicConfig(subTopic);
        String kind = requiredConfigText(config, "kind");
        if ("CIRCUIT".equals(kind)) {
            Map<String, Boolean> inputs = new java.util.HashMap<>();
            if (request.values() != null) {
                request.values().forEach((key, value) -> inputs.put(key, value != null && value != 0));
            }
            boolean answer = request.answer() != null && request.answer().isBoolean() && request.answer().asBoolean();
            return new LogicAttemptRequest("CIRCUIT", com.fasterxml.jackson.databind.node.BooleanNode.valueOf(answer), inputs, null);
        }
        return new LogicAttemptRequest("SIMPLIFY", request.answer(), null, null);
    }

    private String requiredKind(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("kind is required");
        }
        String kind = value.trim();
        if (!Set.of("SIMPLIFY", "CIRCUIT").contains(kind)) {
            throw new ValidationException("kind must be SIMPLIFY or CIRCUIT");
        }
        return kind;
    }

    private String requiredConfigText(JsonNode config, String fieldName) {
        JsonNode value = config.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "LOGIC_FLOW config cannot be graded");
        }
        return value.asText().trim();
    }

    private GradeResult gradeSimplify(JsonNode config, LogicAttemptRequest request) {
        String answer = textAnswer(request.answer());
        if (answer.length() > 1000) {
            throw new ValidationException("answer must be 1000 characters or fewer");
        }
        validateSteps(request.steps());
        LogicExpressionService.Node answerAst = logicExpressionService.parse(answer);
        String expectedExpression = config.hasNonNull("target")
                ? requiredConfigText(config, "target")
                : requiredConfigText(config, "start");
        LogicExpressionService.Node expectedAst = logicExpressionService.parse(expectedExpression);
        boolean correct = logicExpressionService.equivalent(answerAst, expectedAst);
        String normalizedAnswer = logicExpressionService.normalize(answerAst);
        Map<String, Object> details = correct
                ? Map.of("expected", logicExpressionService.normalize(expectedAst), "normalizedAnswer", normalizedAnswer)
                : Map.of("normalizedAnswer", normalizedAnswer);
        return new GradeResult(correct, feedback(config, correct), details);
    }

    private GradeResult gradeCircuit(JsonNode config, LogicAttemptRequest request) {
        if (request.answer() == null || !request.answer().isBoolean()) {
            throw new ValidationException("answer must be a boolean for CIRCUIT");
        }
        if (request.inputs() == null || request.inputs().isEmpty()) {
            throw new ValidationException("inputs are required for CIRCUIT");
        }
        LogicExpressionService.Node expression = logicExpressionService.parse(requiredConfigText(config, "expression"));
        Set<String> variables = logicExpressionService.variables(expression);
        if (!request.inputs().keySet().equals(variables)) {
            throw new ValidationException("inputs must exactly match expression variables");
        }
        boolean expected = logicExpressionService.evaluate(expression, request.inputs());
        boolean answerMatchesCircuit = expected == request.answer().asBoolean();
        String goal = config.path("goal").asText("MATCH_OUTPUT");
        boolean correct = switch (goal) {
            case "TRUE" -> answerMatchesCircuit && expected;
            case "FALSE" -> answerMatchesCircuit && !expected;
            default -> answerMatchesCircuit;
        };
        return new GradeResult(
                correct,
                feedback(config, correct),
                Map.of("expected", expected, "goal", goal, "normalizedAnswer", request.answer().asBoolean())
        );
    }

    private String textAnswer(JsonNode answer) {
        if (answer == null || !answer.isTextual() || answer.asText().isBlank()) {
            throw new ValidationException("answer must be a valid logic expression");
        }
        return answer.asText().trim();
    }

    private void validateSteps(List<LogicStepSubmissionDto> steps) {
        if (steps == null) {
            return;
        }
        if (steps.size() > 20) {
            throw new ValidationException("steps must contain 0 to 20 items");
        }
        Set<String> lawIds = Set.of(
                "DOUBLE_NEGATION", "DE_MORGAN", "DISTRIBUTIVE", "IDENTITY", "DOMINATION", "IDEMPOTENT",
                "COMPLEMENT", "ABSORPTION", "COMMUTATIVE", "ASSOCIATIVE", "IMPLICATION"
        );
        for (LogicStepSubmissionDto step : steps) {
            if (step == null) {
                throw new ValidationException("steps must contain objects");
            }
            if (step.lawId() != null && !step.lawId().isBlank() && !lawIds.contains(step.lawId().trim())) {
                throw new ValidationException("steps lawId must be a known logic law id");
            }
            validateStepExpression(step.from(), "steps.from");
            validateStepExpression(step.to(), "steps.to");
        }
    }

    private void validateStepExpression(String expression, String fieldName) {
        if (expression == null || expression.isBlank()) {
            return;
        }
        if (expression.trim().length() > 1000) {
            throw new ValidationException(fieldName + " must be 1000 characters or fewer");
        }
        logicExpressionService.parse(expression.trim());
    }

    private String feedback(JsonNode config, boolean correct) {
        JsonNode feedback = config.get("feedback");
        if (feedback != null && feedback.isObject()) {
            JsonNode value = feedback.get(correct ? "success" : "failure");
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return correct ? "Correct." : "Not yet. Try again.";
    }

    private boolean isVisualizationMode(SubTopic subTopic) {
        if (subTopic.getInteractionType() == InteractionType.QUIZ) {
            return false;
        }
        if (subTopic.getInteractionConfig() == null || subTopic.getInteractionConfig().isBlank()) {
            return true;
        }
        try {
            JsonNode config = objectMapper.readTree(subTopic.getInteractionConfig());
            if (config.has("mode")) {
                return !"PRACTICE".equalsIgnoreCase(config.get("mode").asText());
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private record GradeResult(boolean correct, String feedback, Map<String, Object> details) {
    }
}
