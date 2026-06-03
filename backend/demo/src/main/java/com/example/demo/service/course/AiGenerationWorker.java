package com.example.demo.service.course;

import com.example.demo.entity.course.AiGenerationLog;
import com.example.demo.entity.course.AiGenerationStatus;
import com.example.demo.entity.course.AiGenerationType;
import com.example.demo.entity.course.ContentDepth;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.DocumentSource;
import com.example.demo.entity.course.InteractionType;
import com.example.demo.entity.course.SubTopic;
import com.example.demo.entity.course.SubTopicSourceType;
import com.example.demo.repository.course.AiGenerationLogRepository;
import com.example.demo.repository.course.CourseModuleRepository;
import com.example.demo.repository.course.CourseRepository;
import com.example.demo.service.exception.ResourceNotFoundException;
import com.example.demo.service.exception.ValidationException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AiGenerationWorker {

    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseRepository courseRepository;
    private final CourseWorkflowGuard workflowGuard;
    private final AiDraftGenerator aiDraftGenerator;
    private final CoursePromptTemplate promptTemplate;
    private final PdfStorageService storageService;
    private final PdfDocumentService pdfDocumentService;
    private final CourseInputValidator validator;
    private final LessonHtmlService lessonHtmlService;
    private final InteractiveConfigService interactiveConfigService;

    AiGenerationWorker(
            AiGenerationLogRepository aiGenerationLogRepository,
            CourseModuleRepository courseModuleRepository,
            CourseRepository courseRepository,
            CourseWorkflowGuard workflowGuard,
            AiDraftGenerator aiDraftGenerator,
            CoursePromptTemplate promptTemplate,
            PdfStorageService storageService,
            PdfDocumentService pdfDocumentService,
            CourseInputValidator validator,
            LessonHtmlService lessonHtmlService,
            InteractiveConfigService interactiveConfigService
    ) {
        this.aiGenerationLogRepository = aiGenerationLogRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseRepository = courseRepository;
        this.workflowGuard = workflowGuard;
        this.aiDraftGenerator = aiDraftGenerator;
        this.promptTemplate = promptTemplate;
        this.storageService = storageService;
        this.pdfDocumentService = pdfDocumentService;
        this.validator = validator;
        this.lessonHtmlService = lessonHtmlService;
        this.interactiveConfigService = interactiveConfigService;
    }

    @Async
    @Transactional
    public void generateModuleDraft(Long logId) {
        AiGenerationLog log = aiGenerationLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("AI generation log not found: " + logId));
        if (log.getStatus() != AiGenerationStatus.PENDING) {
            return;
        }
        if (log.getType() != AiGenerationType.MODULE_DRAFT || log.getModule() == null) {
            log.markFailed("AI generation log is not a module draft request");
            return;
        }

        log.markRunning();
        try {
            CourseModule module = log.getModule();
            workflowGuard.requireAdminEditable(module.getCourse());
            String sourceText = extractSelectedSourceText(log);
            if (sourceText.isBlank()) {
                throw new ValidationException("No extractable text found for selected pages");
            }
            String prompt = promptTemplate.build(
                    module,
                    log.getDocumentSource(),
                    log.getPageStart(),
                    log.getPageEnd(),
                    log.getRequirements(),
                    sourceText
            );
            log.updatePrompt(prompt);
            GeneratedModuleDraft draft = aiDraftGenerator.generateDraft(module, prompt);
            replaceModuleDraftSubTopics(module, log, draft);
            courseModuleRepository.saveAndFlush(module);
            log.markSuccess(draft.rawResponse());
        } catch (RuntimeException ex) {
            log.markFailed(failureMessage(ex));
        }
    }

    @Async
    @Transactional
    public void generateCourseOutline(Long logId) {
        AiGenerationLog log = aiGenerationLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("AI generation log not found: " + logId));
        if (log.getStatus() != AiGenerationStatus.PENDING) {
            return;
        }
        if (log.getType() != AiGenerationType.COURSE_OUTLINE) {
            log.markFailed("AI generation log is not a course outline request");
            return;
        }

        log.markRunning();
        try {
            Course course = log.getCourse();
            workflowGuard.requireAdminEditable(course);
            validateCourseCanAcceptOutline(course);
            String sourceText = extractSelectedSourceText(log);
            if (sourceText.isBlank()) {
                throw new ValidationException("No extractable text found for selected pages");
            }
            String prompt = promptTemplate.buildCourseOutline(
                    course,
                    log.getDocumentSource(),
                    log.getPageStart(),
                    log.getPageEnd(),
                    log.getRequirements(),
                    sourceText
            );
            log.updatePrompt(prompt);
            GeneratedCourseOutlineDraft draft = aiDraftGenerator.generateCourseOutline(course, prompt);
            replaceCourseOutline(course, log, draft);
            courseRepository.saveAndFlush(course);
            log.markSuccess(draft.rawResponse());
        } catch (RuntimeException ex) {
            log.markFailed(failureMessage(ex));
        }
    }

    private String extractSelectedSourceText(AiGenerationLog log) {
        DocumentSource documentSource = log.getDocumentSource();
        try (InputStream pdf = storageService.download(documentSource.getStoragePath())) {
            return pdfDocumentService.extractText(pdf, log.getPageStart(), log.getPageEnd());
        } catch (java.io.IOException ex) {
            throw new ValidationException("Unable to read uploaded PDF from storage");
        }
    }

    private void replaceModuleDraftSubTopics(
            CourseModule module,
            AiGenerationLog log,
            GeneratedModuleDraft draft
    ) {
        if (draft == null || draft.subTopics() == null || draft.subTopics().isEmpty()) {
            throw new ValidationException("AI generator returned no sub-topics");
        }

        List<SubTopic> generatedSubTopics = new ArrayList<>();
        int sortOrder = 0;
        for (GeneratedSubTopicDraft generated : draft.subTopics()) {
            NormalizedGeneratedInteraction interaction = normalizeGeneratedSubTopicInteraction(generated);
            SubTopic subTopic = new SubTopic(
                    validator.requiredText(generated.title(), "generated title", 255),
                    lessonHtmlService.sanitizeForStorage(validator.requiredText(generated.content(), "generated content", 50_000)),
                    sortOrder++,
                    SubTopicSourceType.AI_GENERATED,
                    log.getPageStart(),
                    log.getPageEnd(),
                    interaction.type(),
                    interaction.prompt(),
                    interaction.config()
            );
            generatedSubTopics.add(subTopic);
        }

        module.clearSubTopics();
        generatedSubTopics.forEach(module::addSubTopic);
    }

    private void validateCourseCanAcceptOutline(Course course) {
        boolean hasMeaningfulOutline = course.getModules().stream()
                .anyMatch(module -> !module.getSubTopics().isEmpty()
                        || (module.getDescription() != null && !module.getDescription().isBlank()));
        if (hasMeaningfulOutline) {
            throw new ValidationException("Course already has a meaningful outline");
        }
    }

    private void replaceCourseOutline(Course course, AiGenerationLog log, GeneratedCourseOutlineDraft draft) {
        if (draft == null || draft.modules() == null || draft.modules().isEmpty()) {
            throw new ValidationException("AI generator returned no course modules");
        }

        course.clearModules();
        int moduleSortOrder = 0;
        for (GeneratedCourseModuleDraft generatedModule : draft.modules()) {
            CourseModule module = new CourseModule(
                    validator.requiredText(generatedModule.title(), "generated module title", 255),
                    validator.optionalText(generatedModule.description(), "generated module description", 10_000),
                    moduleSortOrder++,
                    ContentDepth.MEDIUM,
                    defaultInteractionType(generatedModule.interactionType()),
                    validator.optionalText(generatedModule.interactionPrompt(), "generated interactionPrompt", 10_000)
            );
            List<GeneratedSubTopicDraft> generatedSubTopics = generatedModule.subTopics();
            if (generatedSubTopics == null || generatedSubTopics.isEmpty()) {
                throw new ValidationException("AI generator returned a module without sub-topics");
            }
            int subTopicSortOrder = 0;
            for (GeneratedSubTopicDraft generatedSubTopic : generatedSubTopics) {
                NormalizedGeneratedInteraction interaction = normalizeGeneratedSubTopicInteraction(generatedSubTopic);
                module.addSubTopic(new SubTopic(
                        validator.requiredText(generatedSubTopic.title(), "generated sub-topic title", 255),
                        lessonHtmlService.sanitizeForStorage(validator.requiredText(generatedSubTopic.content(), "generated sub-topic content", 50_000)),
                        subTopicSortOrder++,
                        SubTopicSourceType.AI_GENERATED,
                        log.getPageStart(),
                        log.getPageEnd(),
                        interaction.type(),
                        interaction.prompt(),
                        interaction.config()
                ));
            }
            course.addModule(module);
        }
    }

    private NormalizedGeneratedInteraction normalizeGeneratedSubTopicInteraction(GeneratedSubTopicDraft generated) {
        InteractionType type = defaultInteractionType(generated.interactionType());
        String prompt = validator.optionalText(generated.interactionPrompt(), "generated interactionPrompt", 10_000);
        String config = validator.optionalText(generated.interactionConfig(), "generated interactionConfig", 50_000);
        if (type == InteractionType.NONE) {
            return new NormalizedGeneratedInteraction(InteractionType.NONE, prompt, null);
        }
        try {
            String normalizedConfig = interactiveConfigService.validateAndNormalize(type, config);
            return new NormalizedGeneratedInteraction(type, prompt, normalizedConfig);
        } catch (ValidationException ex) {
            return new NormalizedGeneratedInteraction(
                    InteractionType.NONE,
                    validator.optionalText(downgradedInteractionPrompt(type, prompt, ex), "generated interactionPrompt", 10_000),
                    null
            );
        }
    }

    private String downgradedInteractionPrompt(InteractionType type, String prompt, ValidationException ex) {
        String detail = prompt == null || prompt.isBlank() ? "AI suggested an interactive " + type + " activity." : prompt;
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return detail;
        }
        return detail + " Config needs review: " + message;
    }

    private InteractionType defaultInteractionType(InteractionType interactionType) {
        return interactionType == null ? InteractionType.NONE : interactionType;
    }

    private record NormalizedGeneratedInteraction(
            InteractionType type,
            String prompt,
            String config
    ) {
    }

    private String failureMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
