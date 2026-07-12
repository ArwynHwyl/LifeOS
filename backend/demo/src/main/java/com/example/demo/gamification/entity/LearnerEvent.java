package com.example.demo.gamification.entity;

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
import java.time.Instant;

/**
 * Records a gamification event that happened while the learner was not logged in
 * (nightly streak/shield job), so the frontend can show a one-time popup on next login.
 */
@Entity
@Table(name = "learner_event")
public class LearnerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private LearnerEventType eventType;

    @Column(name = "shields_remaining")
    private Integer shieldsRemaining;

    @Column(name = "shield_max")
    private Integer shieldMax;

    @Column(name = "streak_days_lost")
    private Integer streakDaysLost;

    @Column(name = "longest_streak")
    private Integer longestStreak;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    protected LearnerEvent() {
    }

    public static LearnerEvent shieldConsumed(User user, int shieldsRemaining, int shieldMax) {
        LearnerEvent event = new LearnerEvent();
        event.user = user;
        event.eventType = LearnerEventType.SHIELD_CONSUMED;
        event.shieldsRemaining = shieldsRemaining;
        event.shieldMax = shieldMax;
        event.createdAt = Instant.now();
        return event;
    }

    public static LearnerEvent streakLost(User user, int streakDaysLost, int longestStreak) {
        LearnerEvent event = new LearnerEvent();
        event.user = user;
        event.eventType = LearnerEventType.STREAK_LOST;
        event.streakDaysLost = streakDaysLost;
        event.longestStreak = longestStreak;
        event.createdAt = Instant.now();
        return event;
    }

    public void acknowledge() {
        this.acknowledgedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LearnerEventType getEventType() {
        return eventType;
    }

    public Integer getShieldsRemaining() {
        return shieldsRemaining;
    }

    public Integer getShieldMax() {
        return shieldMax;
    }

    public Integer getStreakDaysLost() {
        return streakDaysLost;
    }

    public Integer getLongestStreak() {
        return longestStreak;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }
}
