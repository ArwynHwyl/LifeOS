package com.example.demo.course.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
import com.example.demo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "learner_interactive_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_learner_interactive_progress_user_subtopic",
                columnNames = {"user_id", "sub_topic_id"}
        )
)
public class LearnerInteractiveProgress extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_topic_id", nullable = false)
    private SubTopic subTopic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InteractiveProgressStatus status = InteractiveProgressStatus.NOT_STARTED;

    @Column(nullable = false)
    private int attemptCount;

    private Instant masteredAt;

    protected LearnerInteractiveProgress() {
    }

    public LearnerInteractiveProgress(User user, Course course, SubTopic subTopic) {
        this.user = user;
        this.course = course;
        this.subTopic = subTopic;
    }

    public void recordAttempt(InteractiveProgressStatus requestedStatus) {
        if (requestedStatus == null || requestedStatus == InteractiveProgressStatus.NOT_STARTED) {
            return;
        }
        attemptCount += 1;
        if (status == InteractiveProgressStatus.MASTERED) {
            return;
        }
        status = requestedStatus;
        if (requestedStatus == InteractiveProgressStatus.MASTERED) {
            masteredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Course getCourse() {
        return course;
    }

    public SubTopic getSubTopic() {
        return subTopic;
    }

    public InteractiveProgressStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getMasteredAt() {
        return masteredAt;
    }
}
