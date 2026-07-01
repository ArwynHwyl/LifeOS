package com.example.demo.course.service.management;

import com.example.demo.course.mapper.CourseDtoMapper;
import com.example.demo.course.service.interactive.InteractiveConfigService;
import com.example.demo.course.service.interactive.LessonHtmlService;
import com.example.demo.course.service.learner.UserAccessService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseReview;
import com.example.demo.course.entity.CourseReviewComment;
import com.example.demo.course.repository.AiGenerationLogRepository;
import com.example.demo.course.repository.CourseModuleRepository;
import com.example.demo.course.repository.CourseRepository;
import com.example.demo.course.repository.CourseReviewCommentRepository;
import com.example.demo.course.repository.CourseReviewRepository;
import com.example.demo.course.repository.SubTopicRepository;
import com.example.demo.course.dto.review.response.CourseReviewCommentDto;
import com.example.demo.user.entity.User;
import com.example.demo.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAdminServiceTests {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseModuleRepository courseModuleRepository;

    @Mock
    private SubTopicRepository subTopicRepository;

    @Mock
    private AiGenerationLogRepository aiGenerationLogRepository;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private UserAccessService userAccessService;

    @Mock
    private CourseWorkflowGuard workflowGuard;

    @Mock
    private CourseDtoMapper mapper;

    @Mock
    private CourseInputValidator validator;

    @Mock
    private LessonHtmlService lessonHtmlService;

    @Mock
    private InteractiveConfigService interactiveConfigService;

    @Mock
    private CourseReviewCommentRepository courseReviewCommentRepository;

    @InjectMocks
    private CourseAdminService service;

    @Test
    void setReviewCommentResolvedSuccess() {
        UUID adminId = UUID.randomUUID();
        Long courseId = 1L;
        Long commentId = 2L;

        when(validator.requiredUserId(adminId)).thenReturn(adminId);
        when(validator.requiredId(courseId, "courseId")).thenReturn(courseId);
        when(validator.requiredId(commentId, "commentId")).thenReturn(commentId);

        CourseReviewComment comment = mock(CourseReviewComment.class);
        CourseReview review = mock(CourseReview.class);
        Course course = mock(Course.class);

        when(comment.getReview()).thenReturn(review);
        when(review.getCourse()).thenReturn(course);
        when(course.getId()).thenReturn(courseId);

        when(courseReviewCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(courseReviewCommentRepository.save(comment)).thenReturn(comment);

        Instant now = Instant.now();
        CourseReviewCommentDto expectedDto = new CourseReviewCommentDto(
                commentId, null, null, "Feedback text", now, now, true, now
        );
        when(mapper.toCourseReviewCommentDto(comment)).thenReturn(expectedDto);

        CourseReviewCommentDto result = service.setReviewCommentResolved(adminId, courseId, commentId, true);

        verify(comment).markResolved();
        assertThat(result.resolved()).isTrue();
        assertThat(result.resolvedAt()).isEqualTo(now);
    }

    @Test
    void setReviewCommentResolvedMismatchedCourseThrowsException() {
        UUID adminId = UUID.randomUUID();
        Long courseId = 1L;
        Long mismatchedCourseId = 99L;
        Long commentId = 2L;

        when(validator.requiredUserId(adminId)).thenReturn(adminId);
        when(validator.requiredId(courseId, "courseId")).thenReturn(courseId);
        when(validator.requiredId(commentId, "commentId")).thenReturn(commentId);

        CourseReviewComment comment = mock(CourseReviewComment.class);
        CourseReview review = mock(CourseReview.class);
        Course course = mock(Course.class);

        when(comment.getReview()).thenReturn(review);
        when(review.getCourse()).thenReturn(course);
        // comment belongs to mismatchedCourseId
        when(course.getId()).thenReturn(mismatchedCourseId);

        when(courseReviewCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> service.setReviewCommentResolved(adminId, courseId, commentId, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment not found in course: " + commentId);
    }
}
