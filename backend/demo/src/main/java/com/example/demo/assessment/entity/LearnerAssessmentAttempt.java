package com.example.demo.assessment.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
import com.example.demo.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
        name = "learner_assessment_attempt",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_learner_assessment_attempt_user_assessment",
                columnNames = {"user_id", "assessment_id"}
        )
)
public class LearnerAssessmentAttempt extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private Instant submittedAt;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LearnerAssessmentAnswer> answers = new ArrayList<>();

    protected LearnerAssessmentAttempt() {
    }

    public LearnerAssessmentAttempt(User user, Assessment assessment, int score, int totalQuestions, Instant submittedAt) {
        this.user = user;
        this.assessment = assessment;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.submittedAt = submittedAt;
    }

    public void addAnswer(LearnerAssessmentAnswer answer) {
        answers.add(answer);
        answer.assignToAttempt(this);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public int getScore() {
        return score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public List<LearnerAssessmentAnswer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }
}
