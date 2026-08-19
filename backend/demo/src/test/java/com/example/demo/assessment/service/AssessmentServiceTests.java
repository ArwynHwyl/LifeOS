package com.example.demo.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.assessment.dto.AnswerSubmission;
import com.example.demo.assessment.dto.AssessmentDetailDto;
import com.example.demo.assessment.dto.AssessmentResultDto;
import com.example.demo.assessment.dto.AssessmentSummaryDto;
import com.example.demo.assessment.dto.SubmitAssessmentRequest;
import com.example.demo.assessment.entity.Assessment;
import com.example.demo.assessment.entity.AssessmentOption;
import com.example.demo.assessment.entity.AssessmentQuestion;
import com.example.demo.assessment.entity.LearnerAssessmentAttempt;
import com.example.demo.assessment.repository.AssessmentQuestionRepository;
import com.example.demo.assessment.repository.AssessmentRepository;
import com.example.demo.assessment.repository.LearnerAssessmentAttemptRepository;
import com.example.demo.gamification.dto.GamificationRewardDto;
import com.example.demo.gamification.service.GamificationService;
import com.example.demo.shared.exception.InvalidWorkflowStateException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTests {

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private AssessmentQuestionRepository assessmentQuestionRepository;

    @Mock
    private LearnerAssessmentAttemptRepository attemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private AssessmentService service;

    private Assessment mockAssessment(Long id, String title, Integer sortOrder) {
        Assessment assessment = mock(Assessment.class);
        lenient().when(assessment.getId()).thenReturn(id);
        lenient().when(assessment.getTitle()).thenReturn(title);
        lenient().when(assessment.getDescription()).thenReturn("Practice set");
        lenient().when(assessment.getTag()).thenReturn("ALGEBRA");
        lenient().when(assessment.getSortOrder()).thenReturn(sortOrder);
        return assessment;
    }

    private AssessmentQuestion mockQuestion(Long questionId, String text, AssessmentOption... options) {
        AssessmentQuestion question = mock(AssessmentQuestion.class);
        lenient().when(question.getId()).thenReturn(questionId);
        lenient().when(question.getQuestionText()).thenReturn(text);
        lenient().when(question.getOptions()).thenReturn(List.of(options));
        return question;
    }

    private AssessmentOption mockOption(Long optionId, String text, boolean correct) {
        AssessmentOption option = mock(AssessmentOption.class);
        lenient().when(option.getId()).thenReturn(optionId);
        lenient().when(option.getOptionText()).thenReturn(text);
        lenient().when(option.isCorrect()).thenReturn(correct);
        return option;
    }

    // UTC-15_TC-01
    @Test
    void listAssessmentsMapsUnattemptedAssessmentWithoutScore() {
        UUID userId = UUID.randomUUID();
        Assessment assessment = mockAssessment(1L, "Linear Equations", 1);

        when(assessmentRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(assessment));
        when(assessmentQuestionRepository.countByAssessmentId(1L)).thenReturn(5L);
        when(attemptRepository.findByUserUserIdAndAssessmentId(userId, 1L)).thenReturn(Optional.empty());

        List<AssessmentSummaryDto> result = service.listAssessments(userId);

        assertThat(result).hasSize(1);
        AssessmentSummaryDto dto = result.get(0);
        assertThat(dto.attempted()).isFalse();
        assertThat(dto.score()).isNull();
        assertThat(dto.totalQuestions()).isNull();
        assertThat(dto.questionCount()).isEqualTo(5L);
    }

    // UTC-15_TC-02
    @Test
    void listAssessmentsMapsAttemptedAssessmentWithSavedScore() {
        UUID userId = UUID.randomUUID();
        Assessment assessment = mockAssessment(2L, "Quadratics", 2);
        LearnerAssessmentAttempt attempt = mock(LearnerAssessmentAttempt.class);
        when(attempt.getScore()).thenReturn(4);
        when(attempt.getTotalQuestions()).thenReturn(5);

        when(assessmentRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(assessment));
        when(assessmentQuestionRepository.countByAssessmentId(2L)).thenReturn(5L);
        when(attemptRepository.findByUserUserIdAndAssessmentId(userId, 2L)).thenReturn(Optional.of(attempt));

        List<AssessmentSummaryDto> result = service.listAssessments(userId);

        AssessmentSummaryDto dto = result.get(0);
        assertThat(dto.attempted()).isTrue();
        assertThat(dto.score()).isEqualTo(4);
        assertThat(dto.totalQuestions()).isEqualTo(5);
    }

    // UTC-15_TC-03
    @Test
    void getAssessmentDetailReturnsQuestionsWithoutCorrectFlagWhenNotAttempted() {
        UUID userId = UUID.randomUUID();
        AssessmentOption correct = mockOption(101L, "x = 2", true);
        AssessmentOption wrong = mockOption(102L, "x = 3", false);
        AssessmentQuestion question = mockQuestion(1L, "Solve for x", correct, wrong);
        Assessment assessment = mockAssessment(1L, "Linear Equations", 1);
        when(assessment.getQuestions()).thenReturn(List.of(question));

        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(attemptRepository.findByUserUserIdAndAssessmentId(userId, 1L)).thenReturn(Optional.empty());

        AssessmentDetailDto detail = service.getAssessmentDetail(userId, 1L);

        assertThat(detail.attempted()).isFalse();
        assertThat(detail.result()).isNull();
        assertThat(detail.questions()).hasSize(1);
        assertThat(detail.questions().get(0).options()).hasSize(2);
    }

    // UTC-15_TC-04
    @Test
    void getAssessmentDetailReturnsGradedResultWhenAlreadyAttempted() {
        UUID userId = UUID.randomUUID();
        AssessmentOption correct = mockOption(101L, "x = 2", true);
        AssessmentQuestion question = mockQuestion(1L, "Solve for x", correct);
        Assessment assessment = mockAssessment(1L, "Linear Equations", 1);
        when(assessment.getQuestions()).thenReturn(List.of(question));

        User user = mock(User.class);
        LearnerAssessmentAttempt attempt = new LearnerAssessmentAttempt(user, assessment, 1, 1, java.time.Instant.now());
        attempt.addAnswer(new com.example.demo.assessment.entity.LearnerAssessmentAnswer(question, correct, true));

        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(attemptRepository.findByUserUserIdAndAssessmentId(userId, 1L)).thenReturn(Optional.of(attempt));

        AssessmentDetailDto detail = service.getAssessmentDetail(userId, 1L);

        assertThat(detail.attempted()).isTrue();
        assertThat(detail.questions()).isNull();
        assertThat(detail.result()).isNotNull();
        assertThat(detail.result().score()).isEqualTo(1);
        assertThat(detail.result().percentage()).isEqualTo(100.0);
    }

    // UTC-15_TC-05
    @Test
    void getAssessmentDetailUnknownIdThrowsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(assessmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAssessmentDetail(userId, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Assessment not found: 99");
    }

    // UTC-16_TC-01
    @Test
    void submitAssessmentGradesMixedAnswersAndSavesAttemptWithoutReward() {
        UUID userId = UUID.randomUUID();
        Long assessmentId = 5L;

        AssessmentOption q1Correct = mockOption(101L, "x = 2", true);
        AssessmentOption q1Wrong = mockOption(102L, "x = 3", false);
        AssessmentQuestion q1 = mockQuestion(1L, "Solve for x", q1Correct, q1Wrong);

        AssessmentOption q2Correct = mockOption(201L, "y = 4", true);
        AssessmentOption q2Wrong = mockOption(202L, "y = 5", false);
        AssessmentQuestion q2 = mockQuestion(2L, "Solve for y", q2Correct, q2Wrong);

        Assessment assessment = mockAssessment(assessmentId, "Linear Equations", 1);
        when(assessment.getQuestions()).thenReturn(List.of(q1, q2));

        when(attemptRepository.existsByUserUserIdAndAssessmentId(userId, assessmentId)).thenReturn(false);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));
        when(attemptRepository.save(any(LearnerAssessmentAttempt.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitAssessmentRequest request = new SubmitAssessmentRequest(List.of(
                new AnswerSubmission(1L, 101L),
                new AnswerSubmission(2L, 202L)
        ));

        AssessmentResultDto result = service.submitAssessment(userId, assessmentId, request);

        assertThat(result.score()).isEqualTo(1);
        assertThat(result.totalQuestions()).isEqualTo(2);
        assertThat(result.percentage()).isEqualTo(50.0);
        assertThat(result.reward()).isEqualTo(GamificationRewardDto.empty());
        verify(gamificationService, never()).notifyAssessmentPerfect(any(), anyInt());
    }

    // UTC-16_TC-02
    @Test
    void submitAssessmentPerfectScoreNotifiesGamificationService() {
        UUID userId = UUID.randomUUID();
        Long assessmentId = 6L;

        AssessmentOption correct = mockOption(101L, "x = 2", true);
        AssessmentQuestion question = mockQuestion(1L, "Solve for x", correct);
        Assessment assessment = mockAssessment(assessmentId, "Linear Equations", 1);
        when(assessment.getQuestions()).thenReturn(List.of(question));

        when(attemptRepository.existsByUserUserIdAndAssessmentId(userId, assessmentId)).thenReturn(false);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));
        when(attemptRepository.save(any(LearnerAssessmentAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attemptRepository.countPerfectByUserId(userId)).thenReturn(1L);
        GamificationRewardDto reward = new GamificationRewardDto(15, false, 3, List.of());
        when(gamificationService.notifyAssessmentPerfect(userId, 1)).thenReturn(reward);

        SubmitAssessmentRequest request = new SubmitAssessmentRequest(List.of(new AnswerSubmission(1L, 101L)));

        AssessmentResultDto result = service.submitAssessment(userId, assessmentId, request);

        assertThat(result.score()).isEqualTo(result.totalQuestions());
        assertThat(result.reward()).isEqualTo(reward);
        verify(gamificationService).notifyAssessmentPerfect(userId, 1);
    }

    // UTC-16_TC-03
    @Test
    void submitAssessmentDuplicateAttemptThrowsInvalidWorkflowStateException() {
        UUID userId = UUID.randomUUID();
        Long assessmentId = 7L;
        when(attemptRepository.existsByUserUserIdAndAssessmentId(userId, assessmentId)).thenReturn(true);

        SubmitAssessmentRequest request = new SubmitAssessmentRequest(List.of(new AnswerSubmission(1L, 101L)));

        assertThatThrownBy(() -> service.submitAssessment(userId, assessmentId, request))
                .isInstanceOf(InvalidWorkflowStateException.class)
                .hasMessageContaining("Assessment " + assessmentId + " has already been submitted");

        verify(attemptRepository, never()).save(any());
        verify(gamificationService, never()).notifyAssessmentPerfect(any(), anyInt());
    }

    // UTC-16_TC-04
    @Test
    void submitAssessmentOptionNotBelongingToQuestionThrowsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        Long assessmentId = 8L;

        AssessmentOption correct = mockOption(101L, "x = 2", true);
        AssessmentQuestion question = mockQuestion(1L, "Solve for x", correct);
        Assessment assessment = mockAssessment(assessmentId, "Linear Equations", 1);
        when(assessment.getQuestions()).thenReturn(List.of(question));

        when(attemptRepository.existsByUserUserIdAndAssessmentId(userId, assessmentId)).thenReturn(false);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        SubmitAssessmentRequest request = new SubmitAssessmentRequest(List.of(new AnswerSubmission(1L, 999L)));

        assertThatThrownBy(() -> service.submitAssessment(userId, assessmentId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Option 999 does not belong to question 1");

        verify(attemptRepository, never()).save(any());
    }
}
