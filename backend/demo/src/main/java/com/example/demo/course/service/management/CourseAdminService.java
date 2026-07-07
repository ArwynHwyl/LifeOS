package com.example.demo.course.service.management;

import com.example.demo.course.dto.management.request.CourseCreateRequest;
import com.example.demo.course.dto.management.request.CourseUpdateRequest;
import com.example.demo.course.dto.management.request.ModuleCreateRequest;
import com.example.demo.course.dto.management.request.ModuleUpdateRequest;
import com.example.demo.course.dto.management.request.SubTopicCreateRequest;
import com.example.demo.course.dto.management.request.SubTopicUpdateRequest;
import com.example.demo.course.dto.management.response.CourseDetailDto;
import com.example.demo.course.dto.management.response.CourseSummaryDto;
import com.example.demo.course.dto.management.response.ModuleDto;
import com.example.demo.course.dto.management.response.SubTopicDto;
import com.example.demo.course.dto.review.response.CourseReviewCommentDto;
import com.example.demo.course.dto.review.response.CourseReviewDto;
import com.example.demo.course.entity.ContentDepth;
import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseModule;
import com.example.demo.course.entity.CourseReviewComment;
import com.example.demo.course.entity.InteractionType;
import com.example.demo.course.entity.SubTopic;
import com.example.demo.course.entity.SubTopicSourceType;
import com.example.demo.course.mapper.CourseDtoMapper;
import com.example.demo.course.repository.AiGenerationLogRepository;
import com.example.demo.course.repository.CourseModuleRepository;
import com.example.demo.course.repository.CourseRepository;
import com.example.demo.course.repository.CourseReviewCommentRepository;
import com.example.demo.course.repository.CourseReviewRepository;
import com.example.demo.course.repository.SubTopicRepository;
import com.example.demo.course.service.interactive.InteractiveConfigService;
import com.example.demo.course.service.interactive.LessonHtmlService;
import com.example.demo.course.service.learner.UserAccessService;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.exception.ValidationException;
import com.example.demo.user.entity.User;
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
        requireAdmin(adminUserId);
        return mapper.toSummaryDtos(courseRepository.findAllByOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public CourseDetailDto getCourse(UUID adminUserId, Long courseId) {
        requireAdmin(adminUserId);
        return mapper.toDetailDto(findCourse(courseId));
    }

    @Transactional(readOnly = true)
    public List<CourseReviewDto> getCourseReviews(UUID adminUserId, Long courseId) {
        requireAdmin(adminUserId);
        return courseReviewRepository.findByCourseIdOrderByCreatedAtDescIdDesc(validator.requiredId(courseId, "courseId")).stream()
                .map(mapper::toCourseReviewDto)
                .toList();
    }

    @Transactional
    public CourseDetailDto createCourse(UUID adminUserId, CourseCreateRequest request) {
        requireRequest(request, "Course create request is required");
        User admin = requireAdmin(adminUserId);
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
        requireAdmin(adminUserId);
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
        requireAdmin(adminUserId);
        Course course = findCourse(courseId);
        workflowGuard.prepareForAdminEdit(course);
        aiGenerationLogRepository.deleteByCourseId(course.getId());
        courseReviewRepository.deleteByCourseId(course.getId());
        courseRepository.delete(course);
    }

    @Transactional
    public CourseDetailDto submitForReview(UUID adminUserId, Long courseId) {
        requireAdmin(adminUserId);
        Course course = findCourse(courseId);
        workflowGuard.requireAdminEditable(course);
        validateReadyForReview(course);
        course.submitForReview();
        return mapper.toDetailDto(course);
    }

    @Transactional
    public CourseDetailDto publish(UUID adminUserId, Long courseId) {
        requireAdmin(adminUserId);
        throw new ValidationException("Courses are published automatically when a teacher approves the review");
    }

    @Transactional
    public ModuleDto addModule(UUID adminUserId, Long courseId, ModuleCreateRequest request) {
        requireRequest(request, "Module create request is required");
        requireAdmin(adminUserId);
        Course course = findCourse(courseId);
        workflowGuard.prepareForAdminEdit(course);
        CourseModule module = toModule(request);
        course.addModule(module);
        return mapper.toModuleDto(courseModuleRepository.save(module));
    }

    @Transactional
    public ModuleDto updateModule(UUID adminUserId, Long moduleId, ModuleUpdateRequest request) {
        requireRequest(request, "Module update request is required");
        requireAdmin(adminUserId);
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
        requireAdmin(adminUserId);
        CourseModule module = findModule(moduleId);
        Course course = module.getCourse();
        workflowGuard.prepareForAdminEdit(course);
        course.removeModule(module);
        courseModuleRepository.delete(module);
    }

    @Transactional
    public SubTopicDto addSubTopic(UUID adminUserId, Long moduleId, SubTopicCreateRequest request) {
        requireRequest(request, "Sub-topic create request is required");
        requireAdmin(adminUserId);
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
        requireAdmin(adminUserId);
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
        requireAdmin(adminUserId);
        SubTopic subTopic = findSubTopic(subTopicId);
        CourseModule module = subTopic.getModule();
        workflowGuard.prepareForAdminEdit(module.getCourse());
        module.removeSubTopic(subTopic);
    }

    private User requireAdmin(UUID adminUserId) {
        return userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
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

    private ContentDepth requireContentDepth(ContentDepth contentDepth) {
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
        requireAdmin(adminUserId);
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
