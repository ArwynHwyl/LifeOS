package com.example.demo.gamification.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AchievementCriteriaTypeConverter implements AttributeConverter<AchievementCriteriaType, String> {

    @Override
    public String convertToDatabaseColumn(AchievementCriteriaType value) {
        return value == null ? null : value.name();
    }

    @Override
    public AchievementCriteriaType convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        // Existing databases retain the criteria name from before quizzes became assessments.
        if ("QUIZ_PERFECT_COUNT".equals(value)) {
            return AchievementCriteriaType.ASSESSMENT_PERFECT_COUNT;
        }
        return AchievementCriteriaType.valueOf(value);
    }
}
