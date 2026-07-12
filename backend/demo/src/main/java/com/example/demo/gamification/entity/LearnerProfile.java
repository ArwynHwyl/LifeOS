package com.example.demo.gamification.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
import com.example.demo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "learner_profile")
public class LearnerProfile extends BaseTimestampEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private int level = 1;

    @Column(name = "current_exp", nullable = false)
    private int currentExp = 0;

    @Column(name = "total_exp", nullable = false)
    private long totalExp = 0;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(name = "current_shield", nullable = false)
    private int currentShield = 0;

    @Column(name = "shield_pool_last_reset_at")
    private Instant shieldPoolLastResetAt;

    // Last date the streak is confirmed safe due to a shield covering a missed day.
    // Distinct from lastActiveDate (true last engagement) so the nightly job can tell
    // a shield-covered gap from a real one and never double-charge the same missed day.
    @Column(name = "streak_safe_through_date")
    private LocalDate streakSafeThroughDate;

    protected LearnerProfile() {
    }

    // userId is intentionally left null here — it must stay null until Hibernate derives it
    // from `user` via @MapsId during persist(). Setting it eagerly makes Spring Data's isNew()
    // check treat this transient entity as existing, so save() calls merge() instead of
    // persist() and Hibernate throws AssertionFailure: null identifier.
    public LearnerProfile(User user) {
        this.user = user;
    }

    public UUID getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getCurrentExp() {
        return currentExp;
    }

    public void setCurrentExp(int currentExp) {
        this.currentExp = currentExp;
    }

    public long getTotalExp() {
        return totalExp;
    }

    public void addTotalExp(int amount) {
        this.totalExp += amount;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
        if (currentStreak > this.longestStreak) {
            this.longestStreak = currentStreak;
        }
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public LocalDate getLastActiveDate() {
        return lastActiveDate;
    }

    public void setLastActiveDate(LocalDate lastActiveDate) {
        this.lastActiveDate = lastActiveDate;
    }

    public int getCurrentShield() {
        return currentShield;
    }

    public void setCurrentShield(int currentShield) {
        this.currentShield = currentShield;
    }

    public Instant getShieldPoolLastResetAt() {
        return shieldPoolLastResetAt;
    }

    public void setShieldPoolLastResetAt(Instant shieldPoolLastResetAt) {
        this.shieldPoolLastResetAt = shieldPoolLastResetAt;
    }

    public LocalDate getStreakSafeThroughDate() {
        return streakSafeThroughDate;
    }

    public void setStreakSafeThroughDate(LocalDate streakSafeThroughDate) {
        this.streakSafeThroughDate = streakSafeThroughDate;
    }
}
