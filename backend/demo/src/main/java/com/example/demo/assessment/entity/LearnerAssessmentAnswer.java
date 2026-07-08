package com.example.demo.assessment.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "learner_assessment_answer")
public class LearnerAssessmentAnswer extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private LearnerAssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private AssessmentOption selectedOption;

    @Column(nullable = false)
    private boolean correct;

    protected LearnerAssessmentAnswer() {
    }

    public LearnerAssessmentAnswer(AssessmentQuestion question, AssessmentOption selectedOption, boolean correct) {
        this.question = question;
        this.selectedOption = selectedOption;
        this.correct = correct;
    }

    void assignToAttempt(LearnerAssessmentAttempt attempt) {
        this.attempt = attempt;
    }

    public Long getId() {
        return id;
    }

    public LearnerAssessmentAttempt getAttempt() {
        return attempt;
    }

    public AssessmentQuestion getQuestion() {
        return question;
    }

    public AssessmentOption getSelectedOption() {
        return selectedOption;
    }

    public boolean isCorrect() {
        return correct;
    }
}
