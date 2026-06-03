package com.example.demo.service.course;

import com.example.demo.dto.course.PublishedCourseDetailDto;
import com.example.demo.dto.course.PublishedCourseSummaryDto;
import com.example.demo.dto.course.InteractiveProgressDto;
import com.example.demo.dto.course.InteractiveProgressUpdateRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseStatus;
import com.example.demo.entity.course.InteractionType;
import com.example.demo.entity.course.InteractiveProgressStatus;
import com.example.demo.entity.course.LearnerInteractiveProgress;
import com.example.demo.entity.course.SubTopic;
import com.example.demo.repository.course.LearnerInteractiveProgressRepository;
import com.example.demo.repository.course.CourseRepository;
import com.example.demo.service.exception.ResourceNotFoundException;
import com.example.demo.service.exception.ValidationException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearnerCourseService {

    private final CourseRepository courseRepository;
    private final LearnerInteractiveProgressRepository progressRepository;
    private final EntityManager entityManager;
    private final CourseDtoMapper mapper;
    private final CourseInputValidator validator;

    public LearnerCourseService(
            CourseRepository courseRepository,
            LearnerInteractiveProgressRepository progressRepository,
            EntityManager entityManager,
            CourseDtoMapper mapper,
            CourseInputValidator validator
    ) {
        this.courseRepository = courseRepository;
        this.progressRepository = progressRepository;
        this.entityManager = entityManager;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<PublishedCourseSummaryDto> listPublishedCourses() {
        return mapper.toPublishedSummaryDtos(courseRepository.findByStatusOrderByUpdatedAtDesc(CourseStatus.PUBLISHED));
    }

    @Transactional(readOnly = true)
    public PublishedCourseDetailDto getPublishedCourse(UUID userId, Long courseId) {
        validator.requiredId(courseId, "courseId");
        Course course = courseRepository.findById(validator.requiredId(courseId, "courseId"))
                .filter(foundCourse -> foundCourse.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published course not found: " + courseId));
        return mapper.toPublishedDetailDto(course, progressBySubTopicId(userId, interactiveSubTopics(course)));
    }

    @Transactional
    public InteractiveProgressDto updateInteractiveProgress(
            UUID userId,
            Long courseId,
            Long subTopicId,
            InteractiveProgressUpdateRequest request
    ) {
        validator.requiredId(courseId, "courseId");
        validator.requiredId(subTopicId, "subTopicId");
        InteractiveProgressStatus requestedStatus = request.status();
        if (requestedStatus == null || requestedStatus == InteractiveProgressStatus.NOT_STARTED) {
            throw new ValidationException("status must be TRIED or MASTERED");
        }

        Course course = courseRepository.findById(courseId)
                .filter(foundCourse -> foundCourse.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published course not found: " + courseId));
        SubTopic subTopic = interactiveSubTopics(course).stream()
                .filter(item -> item.getId().equals(subTopicId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Interactive subtopic not found in published course: " + subTopicId));

        LearnerInteractiveProgress progress = progressRepository.findByUserUserIdAndSubTopicId(userId, subTopicId)
                .orElseGet(() -> new LearnerInteractiveProgress(
                        entityManager.getReference(User.class, userId),
                        course,
                        subTopic
                ));
        progress.recordAttempt(requestedStatus);
        return toProgressDto(progressRepository.save(progress));
    }

    private List<SubTopic> interactiveSubTopics(Course course) {
        return course.getModules().stream()
                .flatMap(module -> module.getSubTopics().stream())
                .filter(subTopic -> subTopic.getInteractionType() != InteractionType.NONE)
                .toList();
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
}
