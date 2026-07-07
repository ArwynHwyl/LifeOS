package com.example.demo.course.service.generation;

import com.example.demo.course.mapper.CourseDtoMapper;
import com.example.demo.course.service.management.CourseInputValidator;
import com.example.demo.course.service.management.CourseWorkflowGuard;
import com.example.demo.course.service.learner.UserAccessService;

import com.example.demo.course.dto.generation.response.AiGenerationLogDto;
import com.example.demo.course.dto.generation.request.AiGenerationRequest;
import com.example.demo.course.dto.generation.response.AiGenerationResultDto;
import com.example.demo.course.entity.AiGenerationLog;
import com.example.demo.course.entity.AiGenerationType;
import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseModule;
import com.example.demo.course.entity.DocumentSource;
import com.example.demo.course.repository.AiGenerationLogRepository;
import com.example.demo.course.repository.CourseModuleRepository;
import com.example.demo.course.repository.CourseRepository;
import com.example.demo.course.repository.DocumentSourceRepository;
import com.example.demo.user.entity.User;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.exception.ValidationException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AiGenerationService {

    private static final int MAX_EXTRACTION_PAGE_RANGE = 100;

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final DocumentSourceRepository documentSourceRepository;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final UserAccessService userAccessService;
    private final CourseWorkflowGuard workflowGuard;
    private final CoursePromptTemplate promptTemplate;
    private final AiGenerationWorker generationWorker;
    private final CourseDtoMapper mapper;
    private final CourseInputValidator validator;

    public AiGenerationService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            DocumentSourceRepository documentSourceRepository,
            AiGenerationLogRepository aiGenerationLogRepository,
            UserAccessService userAccessService,
            CourseWorkflowGuard workflowGuard,
            CoursePromptTemplate promptTemplate,
            AiGenerationWorker generationWorker,
            CourseDtoMapper mapper,
            CourseInputValidator validator
    ) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.documentSourceRepository = documentSourceRepository;
        this.aiGenerationLogRepository = aiGenerationLogRepository;
        this.userAccessService = userAccessService;
        this.workflowGuard = workflowGuard;
        this.promptTemplate = promptTemplate;
        this.generationWorker = generationWorker;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional
    public AiGenerationResultDto generateModuleDraft(UUID adminUserId, Long moduleId, AiGenerationRequest request) {
        AiGenerationLogDto queuedLog = requestModuleDraftGeneration(adminUserId, moduleId, request);
        return new AiGenerationResultDto(queuedLog, List.of());
    }

    @Transactional
    public AiGenerationLogDto requestModuleDraftGeneration(UUID adminUserId, Long moduleId, AiGenerationRequest request) {
        requireRequest(request);
        User admin = userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        CourseModule module = findModule(moduleId);
        Course course = module.getCourse();
        workflowGuard.prepareForAdminEdit(course);

        DocumentSource documentSource = findDocument(request.documentSourceId());
        validateDocumentBelongsToCourse(documentSource, course);
        validator.validateRequiredPageRange(request.pageStart(), request.pageEnd(), documentSource.getPageCount());
        if (request.pageEnd() - request.pageStart() + 1 > MAX_EXTRACTION_PAGE_RANGE) {
            throw new ValidationException("AI generation page range cannot exceed 100 pages");
        }
        String requirements = validator.requiredText(request.requirements(), "requirements", 10_000);

        String prompt = promptTemplate.build(
                module,
                documentSource,
                request.pageStart(),
                request.pageEnd(),
                requirements,
                "Source text will be extracted asynchronously before generation."
        );
        AiGenerationLog log = new AiGenerationLog(
                course,
                module,
                admin,
                documentSource,
                request.pageStart(),
                request.pageEnd(),
                requirements,
                prompt
        );
        AiGenerationLog savedLog = aiGenerationLogRepository.saveAndFlush(log);
        dispatchAfterCommit(savedLog.getId(), AiGenerationType.MODULE_DRAFT);
        return mapper.toAiGenerationLogDto(savedLog);
    }

    @Transactional
    public AiGenerationLogDto requestCourseOutlineGeneration(UUID adminUserId, Long courseId, AiGenerationRequest request) {
        requireRequest(request);
        User admin = userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        Course course = courseRepository.findById(validator.requiredId(courseId, "courseId"))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        workflowGuard.prepareForAdminEdit(course);

        DocumentSource documentSource = findDocument(request.documentSourceId());
        validateDocumentBelongsToCourse(documentSource, course);
        PageRange pageRange = resolveOptionalPageRange(request.pageStart(), request.pageEnd(), documentSource.getPageCount());
        String adminPrompt = validator.requiredText(request.prompt(), "prompt", 20_000);

        String prompt = promptTemplate.buildCourseOutline(
                course,
                documentSource,
                pageRange.pageStart(),
                pageRange.pageEnd(),
                adminPrompt,
                "Source text will be extracted asynchronously before generation."
        );
        AiGenerationLog log = new AiGenerationLog(
                course,
                admin,
                documentSource,
                pageRange.pageStart(),
                pageRange.pageEnd(),
                adminPrompt,
                prompt
        );
        AiGenerationLog savedLog = aiGenerationLogRepository.saveAndFlush(log);
        dispatchAfterCommit(savedLog.getId(), AiGenerationType.COURSE_OUTLINE);
        return mapper.toAiGenerationLogDto(savedLog);
    }

    @Transactional(readOnly = true)
    public List<AiGenerationLogDto> listLogs(UUID adminUserId, Long courseId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        if (!courseRepository.existsById(validator.requiredId(courseId, "courseId"))) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }
        return aiGenerationLogRepository.findByCourseIdOrderByCreatedAtDescIdDesc(courseId).stream()
                .map(mapper::toAiGenerationLogDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiGenerationLogDto getLog(UUID adminUserId, Long logId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        AiGenerationLog log = aiGenerationLogRepository.findById(validator.requiredId(logId, "logId"))
                .orElseThrow(() -> new ResourceNotFoundException("AI generation log not found: " + logId));
        return mapper.toAiGenerationLogDto(log);
    }

    private void validateDocumentBelongsToCourse(DocumentSource documentSource, Course course) {
        if (!documentSource.getCourse().getId().equals(course.getId())) {
            throw new ValidationException("Document source does not belong to course " + course.getId());
        }
    }

    private CourseModule findModule(Long moduleId) {
        return courseModuleRepository.findById(validator.requiredId(moduleId, "moduleId"))
                .orElseThrow(() -> new ResourceNotFoundException("Course module not found: " + moduleId));
    }

    private DocumentSource findDocument(Long documentId) {
        return documentSourceRepository.findById(validator.requiredId(documentId, "documentSourceId"))
                .orElseThrow(() -> new ResourceNotFoundException("Document source not found: " + documentId));
    }

    private PageRange resolveOptionalPageRange(Integer requestedPageStart, Integer requestedPageEnd, Integer pageCount) {
        if (pageCount == null || pageCount < 1) {
            throw new ValidationException("Document page count is required for AI outline generation");
        }
        int pageStart = requestedPageStart == null ? 1 : requestedPageStart;
        int pageEnd = requestedPageEnd == null ? Math.min(pageCount, MAX_EXTRACTION_PAGE_RANGE) : requestedPageEnd;
        validator.validateRequiredPageRange(pageStart, pageEnd, pageCount);
        if (pageEnd - pageStart + 1 > MAX_EXTRACTION_PAGE_RANGE) {
            throw new ValidationException("AI generation page range cannot exceed 100 pages");
        }
        return new PageRange(pageStart, pageEnd);
    }

    private void dispatchAfterCommit(Long logId, AiGenerationType type) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatch(logId, type);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatch(logId, type);
            }
        });
    }

    private void dispatch(Long logId, AiGenerationType type) {
        if (type == AiGenerationType.COURSE_OUTLINE) {
            generationWorker.generateCourseOutline(logId);
        } else {
            generationWorker.generateModuleDraft(logId);
        }
    }

    private void requireRequest(AiGenerationRequest request) {
        if (request == null) {
            throw new ValidationException("AI generation request is required");
        }
    }

    private record PageRange(Integer pageStart, Integer pageEnd) {
    }
}
