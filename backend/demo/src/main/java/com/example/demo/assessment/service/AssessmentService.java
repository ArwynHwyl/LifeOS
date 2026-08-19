package com.example.demo.assessment.service;

import com.example.demo.assessment.dto.AnswerSubmission;
import com.example.demo.assessment.dto.AssessmentDetailDto;
import com.example.demo.assessment.dto.AssessmentOptionDto;
import com.example.demo.assessment.dto.AssessmentOptionResultDto;
import com.example.demo.assessment.dto.AssessmentQuestionDto;
import com.example.demo.assessment.dto.AssessmentQuestionResultDto;
import com.example.demo.assessment.dto.AssessmentResultDto;
import com.example.demo.assessment.dto.AssessmentSummaryDto;
import com.example.demo.assessment.dto.SubmitAssessmentRequest;
import com.example.demo.assessment.entity.Assessment;
import com.example.demo.assessment.entity.AssessmentOption;
import com.example.demo.assessment.entity.AssessmentQuestion;
import com.example.demo.assessment.entity.LearnerAssessmentAnswer;
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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final LearnerAssessmentAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            AssessmentQuestionRepository assessmentQuestionRepository,
            LearnerAssessmentAttemptRepository attemptRepository,
            UserRepository userRepository,
            GamificationService gamificationService
    ) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.gamificationService = gamificationService;
    }

    @Transactional(readOnly = true)
    public List<AssessmentSummaryDto> listAssessments(UUID userId) {
        return assessmentRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(assessment -> {
                    long questionCount = assessmentQuestionRepository.countByAssessmentId(assessment.getId());
                    return attemptRepository.findByUserUserIdAndAssessmentId(userId, assessment.getId())
                            .map(attempt -> new AssessmentSummaryDto(
                                    assessment.getId(), assessment.getTitle(), assessment.getDescription(),
                                    assessment.getTag(), questionCount, assessment.getSortOrder(),
                                    true, attempt.getScore(), attempt.getTotalQuestions()
                            ))
                            .orElseGet(() -> new AssessmentSummaryDto(
                                    assessment.getId(), assessment.getTitle(), assessment.getDescription(),
                                    assessment.getTag(), questionCount, assessment.getSortOrder(),
                                    false, null, null
                            ));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentDetailDto getAssessmentDetail(UUID userId, Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));

        return attemptRepository.findByUserUserIdAndAssessmentId(userId, assessmentId)
                .map(attempt -> new AssessmentDetailDto(
                        assessment.getId(), assessment.getTitle(), assessment.getDescription(),
                        true, null, toResultDto(assessment, attempt, GamificationRewardDto.empty())
                ))
                .orElseGet(() -> new AssessmentDetailDto(
                        assessment.getId(), assessment.getTitle(), assessment.getDescription(),
                        false, toQuestionDtos(assessment), null
                ));
    }

    @Transactional
    public AssessmentResultDto submitAssessment(UUID userId, Long assessmentId, SubmitAssessmentRequest request) {
        if (attemptRepository.existsByUserUserIdAndAssessmentId(userId, assessmentId)) {
            throw new InvalidWorkflowStateException("Assessment " + assessmentId + " has already been submitted");
        }
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Map<Long, Long> selectedOptionByQuestionId = new HashMap<>();
        for (AnswerSubmission answer : request.answers()) {
            selectedOptionByQuestionId.put(answer.questionId(), answer.selectedOptionId());
        }

        record GradedAnswer(AssessmentQuestion question, AssessmentOption selectedOption, boolean correct) {
        }

        List<GradedAnswer> gradedAnswers = assessment.getQuestions().stream()
                .map(question -> {
                    Long selectedOptionId = selectedOptionByQuestionId.get(question.getId());
                    AssessmentOption selectedOption = null;
                    if (selectedOptionId != null) {
                        selectedOption = question.getOptions().stream()
                                .filter(option -> option.getId().equals(selectedOptionId))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Option " + selectedOptionId + " does not belong to question " + question.getId()));
                    }
                    boolean correct = selectedOption != null && selectedOption.isCorrect();
                    return new GradedAnswer(question, selectedOption, correct);
                })
                .toList();

        int score = (int) gradedAnswers.stream().filter(GradedAnswer::correct).count();
        LearnerAssessmentAttempt attempt = new LearnerAssessmentAttempt(
                user, assessment, score, assessment.getQuestions().size(), Instant.now());
        for (GradedAnswer gradedAnswer : gradedAnswers) {
            attempt.addAnswer(new LearnerAssessmentAnswer(gradedAnswer.question(), gradedAnswer.selectedOption(), gradedAnswer.correct()));
        }

        attempt = attemptRepository.save(attempt);

        GamificationRewardDto reward = GamificationRewardDto.empty();
        if (score == attempt.getTotalQuestions()) {
            long totalPerfect = attemptRepository.countPerfectByUserId(userId);
            reward = gamificationService.notifyAssessmentPerfect(userId, (int) totalPerfect);
        }
        return toResultDto(assessment, attempt, reward);
    }

    private List<AssessmentQuestionDto> toQuestionDtos(Assessment assessment) {
        return assessment.getQuestions().stream()
                .map(question -> new AssessmentQuestionDto(
                        question.getId(),
                        question.getQuestionText(),
                        question.getOptions().stream()
                                .map(option -> new AssessmentOptionDto(option.getId(), option.getOptionText()))
                                .toList()
                ))
                .toList();
    }

    private AssessmentResultDto toResultDto(Assessment assessment, LearnerAssessmentAttempt attempt, GamificationRewardDto reward) {
        Map<Long, LearnerAssessmentAnswer> answerByQuestionId = new HashMap<>();
        for (LearnerAssessmentAnswer answer : attempt.getAnswers()) {
            answerByQuestionId.put(answer.getQuestion().getId(), answer);
        }

        List<AssessmentQuestionResultDto> questionResults = assessment.getQuestions().stream()
                .map(question -> {
                    LearnerAssessmentAnswer answer = answerByQuestionId.get(question.getId());
                    Long selectedOptionId = answer != null && answer.getSelectedOption() != null
                            ? answer.getSelectedOption().getId() : null;
                    List<AssessmentOptionResultDto> options = question.getOptions().stream()
                            .map(option -> new AssessmentOptionResultDto(
                                    option.getId(), option.getOptionText(), option.isCorrect(),
                                    option.getId().equals(selectedOptionId)
                            ))
                            .toList();
                    boolean correct = answer != null && answer.isCorrect();
                    return new AssessmentQuestionResultDto(question.getId(), question.getQuestionText(), correct, options);
                })
                .toList();

        double percentage = attempt.getTotalQuestions() == 0
                ? 0.0
                : (attempt.getScore() * 100.0) / attempt.getTotalQuestions();

        return new AssessmentResultDto(
                assessment.getId(), assessment.getTitle(), attempt.getScore(), attempt.getTotalQuestions(),
                percentage, attempt.getSubmittedAt(), questionResults, reward
        );
    }
}
