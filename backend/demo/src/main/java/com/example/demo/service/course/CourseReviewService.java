package com.example.demo.service.course;

import com.example.demo.dto.course.CourseReviewDetailDto;
import com.example.demo.dto.course.CourseReviewDto;
import com.example.demo.dto.course.CourseSummaryDto;
import com.example.demo.dto.course.ReviewCommentRequest;
import com.example.demo.dto.course.ReviewDecisionRequest;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.CourseReview;
import com.example.demo.entity.course.CourseReviewComment;
import com.example.demo.entity.course.CourseReviewDecision;
import com.example.demo.entity.course.CourseStatus;
import com.example.demo.entity.course.SubTopic;
import com.example.demo.entity.User;
import com.example.demo.repository.course.CourseModuleRepository;
import com.example.demo.repository.course.CourseRepository;
import com.example.demo.repository.course.CourseReviewRepository;
import com.example.demo.repository.course.SubTopicRepository;
import com.example.demo.service.exception.ResourceNotFoundException;
import com.example.demo.service.exception.ValidationException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseReviewService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final SubTopicRepository subTopicRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final UserAccessService userAccessService;
    private final CourseWorkflowGuard workflowGuard;
    private final CourseDtoMapper mapper;
    private final CourseInputValidator validator;

    public CourseReviewService(
            CourseRepository courseRepository,
            CourseModuleRepository courseModuleRepository,
            SubTopicRepository subTopicRepository,
            CourseReviewRepository courseReviewRepository,
            UserAccessService userAccessService,
            CourseWorkflowGuard workflowGuard,
            CourseDtoMapper mapper,
            CourseInputValidator validator
    ) {
        this.courseRepository = courseRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.subTopicRepository = subTopicRepository;
        this.courseReviewRepository = courseReviewRepository;
        this.userAccessService = userAccessService;
        this.workflowGuard = workflowGuard;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryDto> listPendingReviews(UUID teacherUserId) {
        userAccessService.requireTeacher(validator.requiredUserId(teacherUserId));
        return mapper.toSummaryDtos(courseRepository.findByStatusOrderByUpdatedAtDesc(CourseStatus.PENDING_REVIEW));
    }

    @Transactional(readOnly = true)
    public CourseReviewDetailDto getCourseForReview(UUID teacherUserId, Long courseId) {
        userAccessService.requireTeacher(validator.requiredUserId(teacherUserId));
        Course course = findCourse(courseId);
        List<CourseReviewDto> reviews = courseReviewRepository.findByCourseIdOrderByCreatedAtDescIdDesc(courseId).stream()
                .map(mapper::toCourseReviewDto)
                .toList();
        return new CourseReviewDetailDto(mapper.toDetailDto(course), reviews);
    }

    @Transactional
    public CourseReviewDto approve(UUID teacherUserId, Long courseId, ReviewDecisionRequest request) {
        User teacher = userAccessService.requireTeacher(validator.requiredUserId(teacherUserId));
        Course course = findCourse(courseId);
        workflowGuard.requireStatus(course, CourseStatus.PENDING_REVIEW, "Approve");
        course.approve(teacher);
        CourseReview review = createReview(course, teacher, CourseReviewDecision.APPROVED, request);
        return mapper.toCourseReviewDto(courseReviewRepository.save(review));
    }

    @Transactional
    public CourseReviewDto requestRevision(UUID teacherUserId, Long courseId, ReviewDecisionRequest request) {
        User teacher = userAccessService.requireTeacher(validator.requiredUserId(teacherUserId));
        Course course = findCourse(courseId);
        workflowGuard.requireStatus(course, CourseStatus.PENDING_REVIEW, "Request revision");
        if (!hasReviewFeedback(request)) {
            throw new ValidationException("Teacher feedback is required when requesting revision");
        }
        course.markNeedsRevision();
        CourseReview review = createReview(course, teacher, CourseReviewDecision.NEED_REVISION, request);
        return mapper.toCourseReviewDto(courseReviewRepository.save(review));
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(validator.requiredId(courseId, "courseId"))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
    }

    private CourseReview createReview(
            Course course,
            User teacher,
            CourseReviewDecision decision,
            ReviewDecisionRequest request
    ) {
        String feedback = request == null ? null : validator.optionalText(request.feedback(), "feedback", 50_000);
        CourseReview review = new CourseReview(course, teacher, decision, feedback);
        for (ReviewCommentRequest commentRequest : reviewComments(request)) {
            review.addComment(toReviewComment(course, commentRequest));
        }
        return review;
    }

    private CourseReviewComment toReviewComment(Course course, ReviewCommentRequest request) {
        if (request == null) {
            throw new ValidationException("Review comment request is required");
        }
        boolean hasModuleTarget = request.moduleId() != null;
        boolean hasSubTopicTarget = request.subTopicId() != null;
        if (hasModuleTarget == hasSubTopicTarget) {
            throw new ValidationException("Review comment must target exactly one module or sub-topic");
        }

        String feedback = validator.requiredText(request.feedback(), "comment feedback", 50_000);
        if (hasModuleTarget) {
            CourseModule module = courseModuleRepository.findById(validator.requiredId(request.moduleId(), "moduleId"))
                    .orElseThrow(() -> new ResourceNotFoundException("Course module not found: " + request.moduleId()));
            validateModuleBelongsToCourse(module, course);
            return new CourseReviewComment(module, null, feedback);
        }

        SubTopic subTopic = subTopicRepository.findById(validator.requiredId(request.subTopicId(), "subTopicId"))
                .orElseThrow(() -> new ResourceNotFoundException("Sub-topic not found: " + request.subTopicId()));
        validateModuleBelongsToCourse(subTopic.getModule(), course);
        return new CourseReviewComment(null, subTopic, feedback);
    }

    private boolean hasReviewFeedback(ReviewDecisionRequest request) {
        if (request == null) {
            return false;
        }
        if (request.feedback() != null && !request.feedback().isBlank()) {
            return true;
        }
        return reviewComments(request).stream()
                .anyMatch(comment -> comment != null
                        && comment.feedback() != null
                        && !comment.feedback().isBlank());
    }

    private List<ReviewCommentRequest> reviewComments(ReviewDecisionRequest request) {
        return request == null || request.comments() == null ? List.of() : request.comments();
    }

    private void validateModuleBelongsToCourse(CourseModule module, Course course) {
        if (!module.getCourse().getId().equals(course.getId())) {
            throw new ValidationException("Review comment target does not belong to course " + course.getId());
        }
    }
}
