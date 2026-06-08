package com.example.demo.service.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.course.InteractiveProgressDto;
import com.example.demo.dto.course.InteractiveProgressUpdateRequest;
import com.example.demo.dto.course.LogicAttemptRequest;
import com.example.demo.dto.course.LogicAttemptResponse;
import com.example.demo.dto.course.PublishedCourseDetailDto;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.entity.UserStatus;
import com.example.demo.entity.course.ContentDepth;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.InteractionType;
import com.example.demo.entity.course.InteractiveProgressStatus;
import com.example.demo.entity.course.LearnerInteractiveProgress;
import com.example.demo.entity.course.SubTopic;
import com.example.demo.entity.course.SubTopicSourceType;
import com.example.demo.repository.course.CourseRepository;
import com.example.demo.repository.course.LearnerInteractiveProgressRepository;
import com.example.demo.service.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LearnerCourseServiceTests {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LearnerInteractiveProgressRepository progressRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private CourseDtoMapper mapper;

    @Mock
    private CourseInputValidator validator;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LogicExpressionService logicExpressionService;

    @Mock
    private MathExpressionService mathExpressionService;

    @InjectMocks
    private LearnerCourseService service;

    @Test
    void progressUpdateCreatesRowForValidPublishedCourseSubtopic() {
        User user = learner();
        Course course = publishedCourseWithSubTopics(interactiveSubTopic(11L), plainSubTopic(12L));
        when(validator.requiredId(1L, "courseId")).thenReturn(1L);
        when(validator.requiredId(11L, "subTopicId")).thenReturn(11L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(progressRepository.findByUserUserIdAndSubTopicId(user.getUserId(), 11L)).thenReturn(Optional.empty());
        when(entityManager.getReference(User.class, user.getUserId())).thenReturn(user);
        when(progressRepository.save(any(LearnerInteractiveProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InteractiveProgressDto dto = service.updateInteractiveProgress(
                user.getUserId(),
                1L,
                11L,
                new InteractiveProgressUpdateRequest(InteractiveProgressStatus.TRIED)
        );

        assertThat(dto.subTopicId()).isEqualTo(11L);
        assertThat(dto.status()).isEqualTo(InteractiveProgressStatus.TRIED);
        assertThat(dto.attemptCount()).isEqualTo(1);
    }

    @Test
    void progressUpdateRejectsUnpublishedCourse() {
        User user = learner();
        Course draftCourse = new Course("Draft", "Description", user);
        ReflectionTestUtils.setField(draftCourse, "id", 1L);
        when(validator.requiredId(1L, "courseId")).thenReturn(1L);
        when(validator.requiredId(11L, "subTopicId")).thenReturn(11L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(draftCourse));

        assertThatThrownBy(() -> service.updateInteractiveProgress(
                user.getUserId(),
                1L,
                11L,
                new InteractiveProgressUpdateRequest(InteractiveProgressStatus.TRIED)
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void progressUpdateRejectsSubtopicOutsideCourse() {
        User user = learner();
        Course course = publishedCourseWithSubTopics(interactiveSubTopic(11L));
        when(validator.requiredId(1L, "courseId")).thenReturn(1L);
        when(validator.requiredId(99L, "subTopicId")).thenReturn(99L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.updateInteractiveProgress(
                user.getUserId(),
                1L,
                99L,
                new InteractiveProgressUpdateRequest(InteractiveProgressStatus.MASTERED)
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void masteredProgressIsNotDowngradedByLaterTried() {
        User user = learner();
        SubTopic subTopic = interactiveSubTopic(11L);
        Course course = publishedCourseWithSubTopics(subTopic);
        LearnerInteractiveProgress progress = new LearnerInteractiveProgress(user, course, subTopic);
        progress.recordAttempt(InteractiveProgressStatus.MASTERED);
        when(validator.requiredId(1L, "courseId")).thenReturn(1L);
        when(validator.requiredId(11L, "subTopicId")).thenReturn(11L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(progressRepository.findByUserUserIdAndSubTopicId(user.getUserId(), 11L)).thenReturn(Optional.of(progress));
        when(progressRepository.save(progress)).thenReturn(progress);

        InteractiveProgressDto dto = service.updateInteractiveProgress(
                user.getUserId(),
                1L,
                11L,
                new InteractiveProgressUpdateRequest(InteractiveProgressStatus.TRIED)
        );

        assertThat(dto.status()).isEqualTo(InteractiveProgressStatus.MASTERED);
        assertThat(dto.attemptCount()).isEqualTo(2);
    }

    @Test
    void getPublishedCourseIncludesPerUserProgressBySubtopic() {
        User user = learner();
        SubTopic triedSubTopic = interactiveSubTopic(11L);
        SubTopic notStartedSubTopic = interactiveSubTopic(12L);
        Course course = publishedCourseWithSubTopics(triedSubTopic, notStartedSubTopic, plainSubTopic(13L));
        LearnerInteractiveProgress progress = new LearnerInteractiveProgress(user, course, triedSubTopic);
        progress.recordAttempt(InteractiveProgressStatus.TRIED);
        when(validator.requiredId(1L, "courseId")).thenReturn(1L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(progressRepository.findByUserUserIdAndSubTopicIdIn(user.getUserId(), List.of(11L, 12L))).thenReturn(List.of(progress));
        when(mapper.toPublishedDetailDto(eq(course), any())).thenReturn(new PublishedCourseDetailDto(1L, "Course", "Description", course.getPublishedAt(), List.of()));

        service.getPublishedCourse(user.getUserId(), 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, InteractiveProgressDto>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mapper).toPublishedDetailDto(eq(course), captor.capture());
        assertThat(captor.getValue().get(11L).status()).isEqualTo(InteractiveProgressStatus.TRIED);
        assertThat(captor.getValue().get(12L).status()).isEqualTo(InteractiveProgressStatus.NOT_STARTED);
        assertThat(captor.getValue()).doesNotContainKey(13L);
    }

    @Test
    void submitLogicAttemptGradesSimplifyAndRecordsMastered() {
        User user = learner();
        SubTopic subTopic = logicSubTopic(11L, """
                {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Logic","start":"P -> Q","target":"¬P ∨ Q","feedback":{"success":"Correct","failure":"Try again"}}
                """);
        Course course = publishedCourseWithSubTopics(subTopic);
        LearnerCourseService realService = new LearnerCourseService(
                courseRepository,
                progressRepository,
                entityManager,
                mapper,
                validator,
                new ObjectMapper(),
                new LogicExpressionService(),
                new MathExpressionService()
        );
        when(validator.requiredId(1L, "courseId")).thenReturn(1L);
        when(validator.requiredId(11L, "subTopicId")).thenReturn(11L);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(progressRepository.findByUserUserIdAndSubTopicId(user.getUserId(), 11L)).thenReturn(Optional.empty());
        when(entityManager.getReference(User.class, user.getUserId())).thenReturn(user);
        when(progressRepository.save(any(LearnerInteractiveProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LogicAttemptResponse response = realService.submitLogicAttempt(
                user.getUserId(),
                1L,
                11L,
                new LogicAttemptRequest("SIMPLIFY", TextNode.valueOf("!P | Q"), null, null)
        );

        assertThat(response.correct()).isTrue();
        assertThat(response.status()).isEqualTo(InteractiveProgressStatus.MASTERED);
        assertThat(response.attemptCount()).isEqualTo(1);
        assertThat(response.feedback()).isEqualTo("Correct");
    }

    private User learner() {
        return new User("learner@example.com", "learner", "hash", "Learn", "Er", UserRole.ROLE_LEARNER, UserStatus.VERIFY);
    }

    private Course publishedCourseWithSubTopics(SubTopic... subTopics) {
        User admin = new User("admin@example.com", "admin", "hash", "Admin", "User", UserRole.ROLE_ADMIN, UserStatus.VERIFY);
        Course course = new Course("Course", "Description", admin);
        ReflectionTestUtils.setField(course, "id", 1L);
        course.publish();
        CourseModule module = new CourseModule("Module", "Description", 1, ContentDepth.MEDIUM);
        ReflectionTestUtils.setField(module, "id", 1L);
        course.addModule(module);
        for (SubTopic subTopic : subTopics) {
            module.addSubTopic(subTopic);
        }
        return course;
    }

    private SubTopic interactiveSubTopic(Long id) {
        SubTopic subTopic = new SubTopic(
                "Interactive",
                "Content",
                id.intValue(),
                SubTopicSourceType.MANUAL,
                null,
                null,
                InteractionType.QUIZ,
                "Try it",
                "{\"type\":\"QUIZ\"}"
        );
        ReflectionTestUtils.setField(subTopic, "id", id);
        return subTopic;
    }

    private SubTopic logicSubTopic(Long id, String config) {
        SubTopic subTopic = new SubTopic(
                "Logic",
                "Content",
                id.intValue(),
                SubTopicSourceType.MANUAL,
                null,
                null,
                InteractionType.LOGIC_FLOW,
                "Try it",
                config
        );
        ReflectionTestUtils.setField(subTopic, "id", id);
        return subTopic;
    }

    private SubTopic plainSubTopic(Long id) {
        SubTopic subTopic = new SubTopic("Plain", "Content", id.intValue(), SubTopicSourceType.MANUAL, null, null);
        ReflectionTestUtils.setField(subTopic, "id", id);
        return subTopic;
    }
}
