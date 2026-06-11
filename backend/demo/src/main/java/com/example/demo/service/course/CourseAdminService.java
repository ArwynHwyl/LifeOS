package com.example.demo.service.course;

import com.example.demo.dto.course.CourseCreateRequest;
import com.example.demo.dto.course.CourseDetailDto;
import com.example.demo.dto.course.CourseReviewDto;
import com.example.demo.dto.course.CourseSummaryDto;
import com.example.demo.dto.course.CourseUpdateRequest;
import com.example.demo.dto.course.ModuleCreateRequest;
import com.example.demo.dto.course.ModuleDto;
import com.example.demo.dto.course.ModuleUpdateRequest;
import com.example.demo.dto.course.SubTopicCreateRequest;
import com.example.demo.dto.course.SubTopicDto;
import com.example.demo.dto.course.SubTopicUpdateRequest;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.dto.course.CourseReviewCommentDto;
import com.example.demo.entity.course.CourseReviewComment;
import com.example.demo.entity.course.CourseStatus;
import com.example.demo.entity.course.InteractionType;
import com.example.demo.entity.course.SubTopic;
import com.example.demo.entity.course.SubTopicSourceType;
import com.example.demo.entity.User;
import com.example.demo.repository.course.CourseModuleRepository;
import com.example.demo.repository.course.CourseRepository;
import com.example.demo.repository.course.CourseReviewRepository;
import com.example.demo.repository.course.CourseReviewCommentRepository;
import com.example.demo.repository.course.AiGenerationLogRepository;
import com.example.demo.repository.course.SubTopicRepository;
import com.example.demo.service.exception.ResourceNotFoundException;
import com.example.demo.service.exception.ValidationException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseAdminService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final SubTopicRepository subTopicRepository;
    private final AiGenerationLogRepository aiGenerationLogRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final UserAccessService userAccessService;
    private final CourseWorkflowGuard workflowGuard;
    private final CourseDtoMapper mapper;
    private final CourseInputValidator validator;
    private final LessonHtmlService lessonHtmlService;
    private final InteractiveConfigService interactiveConfigService;
    private final CourseReviewCommentRepository courseReviewCommentRepository;

    public CourseAdminService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            SubTopicRepository subTopicRepository,
            AiGenerationLogRepository aiGenerationLogRepository,
            CourseReviewRepository courseReviewRepository,
            UserAccessService userAccessService,
            CourseWorkflowGuard workflowGuard,
            CourseDtoMapper mapper,
            CourseInputValidator validator,
            LessonHtmlService lessonHtmlService,
            InteractiveConfigService interactiveConfigService,
            CourseReviewCommentRepository courseReviewCommentRepository
    ) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.subTopicRepository = subTopicRepository;
        this.aiGenerationLogRepository = aiGenerationLogRepository;
        this.courseReviewRepository = courseReviewRepository;
        this.userAccessService = userAccessService;
        this.workflowGuard = workflowGuard;
        this.mapper = mapper;
        this.validator = validator;
        this.lessonHtmlService = lessonHtmlService;
        this.interactiveConfigService = interactiveConfigService;
        this.courseReviewCommentRepository = courseReviewCommentRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryDto> listCourses(UUID adminUserId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        return mapper.toSummaryDtos(courseRepository.findAllByOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public CourseDetailDto getCourse(UUID adminUserId, Long courseId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        return mapper.toDetailDto(findCourse(courseId));
    }

    @Transactional(readOnly = true)
    public List<CourseReviewDto> getCourseReviews(UUID adminUserId, Long courseId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        return courseReviewRepository.findByCourseIdOrderByCreatedAtDescIdDesc(validator.requiredId(courseId, "courseId")).stream()
                .map(mapper::toCourseReviewDto)
                .toList();
    }

    @Transactional
    public CourseDetailDto createCourse(UUID adminUserId, CourseCreateRequest request) {
        requireRequest(request, "Course create request is required");
        User admin = userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        Course course = new Course(
                validator.requiredText(request.title(), "title", 255),
                validator.optionalText(request.description(), "description", 10_000),
                admin
        );
        course.updateCover(validator.optionalText(request.coverId(), "coverId", 80));
        for (ModuleCreateRequest moduleRequest : nullToList(request.modules())) {
            course.addModule(toModule(moduleRequest));
        }
        return mapper.toDetailDto(courseRepository.save(course));
    }

    @Transactional
    public CourseDetailDto updateCourse(UUID adminUserId, Long courseId, CourseUpdateRequest request) {
        requireRequest(request, "Course update request is required");
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        Course course = findCourse(courseId);
        workflowGuard.prepareForAdminEdit(course);
        course.updateDetails(
                validator.requiredText(request.title(), "title", 255),
                validator.optionalText(request.description(), "description", 10_000)
        );
        if (request.coverId() != null) {
            course.updateCover(validator.optionalText(request.coverId(), "coverId", 80));
        }
        return mapper.toDetailDto(course);
    }

    @Transactional
    public void deleteCourse(UUID adminUserId, Long courseId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        Course course = findCourse(courseId);
        workflowGuard.prepareForAdminEdit(course);
        aiGenerationLogRepository.deleteByCourseId(course.getId());
        courseReviewRepository.deleteByCourseId(course.getId());
        courseRepository.delete(course);
    }

    @Transactional
    public CourseDetailDto submitForReview(UUID adminUserId, Long courseId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        Course course = findCourse(courseId);
        workflowGuard.requireAdminEditable(course);
        validateReadyForReview(course);
        course.submitForReview();
        return mapper.toDetailDto(course);
    }

    @Transactional
    public CourseDetailDto publish(UUID adminUserId, Long courseId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        throw new ValidationException("Courses are published automatically when a teacher approves the review");
    }

    @Transactional
    public ModuleDto addModule(UUID adminUserId, Long courseId, ModuleCreateRequest request) {
        requireRequest(request, "Module create request is required");
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        Course course = findCourse(courseId);
        workflowGuard.prepareForAdminEdit(course);
        CourseModule module = toModule(request);
        course.addModule(module);
        return mapper.toModuleDto(courseModuleRepository.save(module));
    }

    @Transactional
    public ModuleDto updateModule(UUID adminUserId, Long moduleId, ModuleUpdateRequest request) {
        requireRequest(request, "Module update request is required");
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        CourseModule module = findModule(moduleId);
        workflowGuard.prepareForAdminEdit(module.getCourse());
        module.updateDetails(
                validator.requiredText(request.title(), "title", 255),
                validator.optionalText(request.description(), "description", 10_000),
                validator.requiredSortOrder(request.sortOrder(), "sortOrder"),
                requireContentDepth(request.contentDepth())
        );
        module.updateInteraction(
                defaultInteractionType(request.interactionType()),
                validator.optionalText(request.interactionPrompt(), "interactionPrompt", 10_000),
                validateInteractionConfig(request.interactionType(), request.interactionConfig())
        );
        return mapper.toModuleDto(module);
    }

    @Transactional
    public void deleteModule(UUID adminUserId, Long moduleId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        CourseModule module = findModule(moduleId);
        Course course = module.getCourse();
        workflowGuard.prepareForAdminEdit(course);
        course.removeModule(module);
        courseModuleRepository.delete(module);
    }

    @Transactional
    public SubTopicDto addSubTopic(UUID adminUserId, Long moduleId, SubTopicCreateRequest request) {
        requireRequest(request, "Sub-topic create request is required");
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        CourseModule module = findModule(moduleId);
        workflowGuard.prepareForAdminEdit(module.getCourse());
        validator.validateOptionalPageRange(request.pageStart(), request.pageEnd());
        SubTopic subTopic = toSubTopic(request);
        module.addSubTopic(subTopic);
        return mapper.toSubTopicDto(subTopicRepository.save(subTopic));
    }

    @Transactional
    public SubTopicDto updateSubTopic(UUID adminUserId, Long subTopicId, SubTopicUpdateRequest request) {
        requireRequest(request, "Sub-topic update request is required");
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        SubTopic subTopic = findSubTopic(subTopicId);
        workflowGuard.prepareForAdminEdit(subTopic.getModule().getCourse());
        validator.validateOptionalPageRange(request.pageStart(), request.pageEnd());
        subTopic.updateDetails(
                validator.requiredText(request.title(), "title", 255),
                lessonHtmlService.sanitizeForStorage(validator.optionalText(request.content(), "content", 100_000)),
                validator.requiredSortOrder(request.sortOrder(), "sortOrder"),
                request.pageStart(),
                request.pageEnd()
        );
        subTopic.updateMascotPrompt(validator.optionalText(request.mascotPrompt(), "mascotPrompt", 1_000));
        subTopic.updateInteraction(
                defaultInteractionType(request.interactionType()),
                validator.optionalText(request.interactionPrompt(), "interactionPrompt", 10_000),
                validateInteractionConfig(request.interactionType(), request.interactionConfig())
        );
        return mapper.toSubTopicDto(subTopic);
    }

    @Transactional
    public void deleteSubTopic(UUID adminUserId, Long subTopicId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        SubTopic subTopic = findSubTopic(subTopicId);
        CourseModule module = subTopic.getModule();
        workflowGuard.prepareForAdminEdit(module.getCourse());
        module.removeSubTopic(subTopic);
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(validator.requiredId(courseId, "courseId"))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
    }

    private CourseModule findModule(Long moduleId) {
        return courseModuleRepository.findById(validator.requiredId(moduleId, "moduleId"))
                .orElseThrow(() -> new ResourceNotFoundException("Course module not found: " + moduleId));
    }

    private SubTopic findSubTopic(Long subTopicId) {
        return subTopicRepository.findById(validator.requiredId(subTopicId, "subTopicId"))
                .orElseThrow(() -> new ResourceNotFoundException("Sub-topic not found: " + subTopicId));
    }

    private CourseModule toModule(ModuleCreateRequest request) {
        requireRequest(request, "Module create request is required");
        CourseModule module = new CourseModule(
                validator.requiredText(request.title(), "title", 255),
                validator.optionalText(request.description(), "description", 10_000),
                validator.requiredSortOrder(request.sortOrder(), "sortOrder"),
                requireContentDepth(request.contentDepth()),
                defaultInteractionType(request.interactionType()),
                validator.optionalText(request.interactionPrompt(), "interactionPrompt", 10_000),
                validateInteractionConfig(request.interactionType(), request.interactionConfig())
        );
        for (SubTopicCreateRequest subTopicRequest : nullToList(request.subTopics())) {
            validator.validateOptionalPageRange(subTopicRequest.pageStart(), subTopicRequest.pageEnd());
            module.addSubTopic(toSubTopic(subTopicRequest));
        }
        return module;
    }

    private SubTopic toSubTopic(SubTopicCreateRequest request) {
        requireRequest(request, "Sub-topic create request is required");
        SubTopic subTopic = new SubTopic(
                validator.requiredText(request.title(), "title", 255),
                lessonHtmlService.sanitizeForStorage(validator.optionalText(request.content(), "content", 100_000)),
                validator.requiredSortOrder(request.sortOrder(), "sortOrder"),
                SubTopicSourceType.MANUAL,
                request.pageStart(),
                request.pageEnd(),
                defaultInteractionType(request.interactionType()),
                validator.optionalText(request.interactionPrompt(), "interactionPrompt", 10_000),
                validateInteractionConfig(request.interactionType(), request.interactionConfig())
        );
        subTopic.updateMascotPrompt(validator.optionalText(request.mascotPrompt(), "mascotPrompt", 1_000));
        return subTopic;
    }

    private void validateReadyForReview(Course course) {
        if (course.getModules().isEmpty()) {
            throw new ValidationException("Course must contain at least one module before review submission");
        }
        for (CourseModule module : course.getModules()) {
            if (module.getSubTopics().isEmpty()) {
                throw new ValidationException("Each module must contain at least one sub-topic before review submission");
            }
            for (SubTopic subTopic : module.getSubTopics()) {
                if (subTopic.getContent() == null || subTopic.getContent().isBlank()) {
                    throw new ValidationException("Each sub-topic must include content before review submission");
                }
            }
        }
    }

    private com.example.demo.entity.course.ContentDepth requireContentDepth(
            com.example.demo.entity.course.ContentDepth contentDepth
    ) {
        if (contentDepth == null) {
            throw new ValidationException("contentDepth is required");
        }
        return contentDepth;
    }

    private InteractionType defaultInteractionType(InteractionType interactionType) {
        return interactionType == null ? InteractionType.NONE : interactionType;
    }

    private String validateInteractionConfig(InteractionType interactionType, String interactionConfig) {
        return interactiveConfigService.validateAndNormalize(defaultInteractionType(interactionType), interactionConfig);
    }

    private void requireRequest(Object request, String message) {
        if (request == null) {
            throw new ValidationException(message);
        }
    }

    private <T> List<T> nullToList(List<T> values) {
        return values == null ? List.of() : values;
    }

    @Transactional
    public CourseReviewCommentDto setReviewCommentResolved(
            UUID adminUserId,
            Long courseId,
            Long commentId,
            boolean resolved
    ) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        validator.requiredId(courseId, "courseId");
        validator.requiredId(commentId, "commentId");

        CourseReviewComment comment = courseReviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        if (!comment.getReview().getCourse().getId().equals(courseId)) {
            throw new ResourceNotFoundException("Comment not found in course: " + commentId);
        }

        if (resolved) {
            comment.markResolved();
        } else {
            comment.markUnresolved();
        }

        return mapper.toCourseReviewCommentDto(courseReviewCommentRepository.save(comment));
    }
}
