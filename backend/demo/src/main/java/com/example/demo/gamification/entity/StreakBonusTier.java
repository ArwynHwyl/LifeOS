package com.example.demo.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "streak_bonus_tier")
public class StreakBonusTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_streak_days", nullable = false)
    private int minStreakDays;

    @Column(name = "bonus_percent", nullable = false)
    private int bonusPercent;

    protected StreakBonusTier() {
    }

    public StreakBonusTier(int minStreakDays, int bonusPercent) {
        this.minStreakDays = minStreakDays;
        this.bonusPercent = bonusPercent;
    }

    public Long getId() {
        return id;
    }

    public int getMinStreakDays() {
        return minStreakDays;
    }

    public int getBonusPercent() {
        return bonusPercent;
    }
}
