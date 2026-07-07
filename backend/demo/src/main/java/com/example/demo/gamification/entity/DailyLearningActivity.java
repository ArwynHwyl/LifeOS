package com.example.demo.gamification.entity;

import com.example.demo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "daily_learning_activity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_learning_activity_user_date",
                columnNames = {"user_id", "activity_date"}
        )
)
public class DailyLearningActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "subtopics_completed", nullable = false)
    private int subtopicsCompleted = 0;

    @Column(name = "exp_earned", nullable = false)
    private int expEarned = 0;

    protected DailyLearningActivity() {
    }

    public DailyLearningActivity(User user, LocalDate activityDate) {
        this.user = user;
        this.activityDate = activityDate;
    }

    public void recordSubtopicCompletion(int expGained) {
        this.subtopicsCompleted += 1;
        this.expEarned += expGained;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getActivityDate() {
        return activityDate;
    }

    public int getSubtopicsCompleted() {
        return subtopicsCompleted;
    }

    public int getExpEarned() {
        return expEarned;
    }
}
