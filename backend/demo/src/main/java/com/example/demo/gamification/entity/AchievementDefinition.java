package com.example.demo.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "achievement_definition")
public class AchievementDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "icon_glyph", length = 10)
    private String iconGlyph;

    @Column(name = "exp_reward", nullable = false)
    private int expReward;

    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_type", nullable = false, length = 40)
    private AchievementCriteriaType criteriaType;

    @Column(name = "criteria_value", nullable = false)
    private int criteriaValue;

    protected AchievementDefinition() {
    }

    public AchievementDefinition(String code, String name, String description, String iconGlyph,
                                  int expReward, AchievementCriteriaType criteriaType, int criteriaValue) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.iconGlyph = iconGlyph;
        this.expReward = expReward;
        this.criteriaType = criteriaType;
        this.criteriaValue = criteriaValue;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconGlyph() {
        return iconGlyph;
    }

    public int getExpReward() {
        return expReward;
    }

    public AchievementCriteriaType getCriteriaType() {
        return criteriaType;
    }

    public int getCriteriaValue() {
        return criteriaValue;
    }
}
