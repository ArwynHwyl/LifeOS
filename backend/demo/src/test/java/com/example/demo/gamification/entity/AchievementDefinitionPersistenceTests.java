package com.example.demo.gamification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.gamification.repository.AchievementDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class AchievementDefinitionPersistenceTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AchievementDefinitionRepository repository;

    @Test
    void loadsLegacyQuizCriteriaAlongsideCurrentCriteria() {
        entityManager.getEntityManager().createNativeQuery("""
                INSERT INTO achievement_definition
                    (code, name, description, exp_reward, criteria_type, criteria_value)
                VALUES ('QUIZ_ROOKIE', 'Quiz Rookie', 'Legacy achievement', 15, 'QUIZ_PERFECT_COUNT', 1)
                """).executeUpdate();
        entityManager.persistAndFlush(new AchievementDefinition(
                "FIRST_STEPS", "First Steps", "Complete a lesson", "*", 20,
                AchievementCriteriaType.FIRST_SUBTOPIC_COMPLETED, 1));
        entityManager.clear();

        assertThat(repository.findAll()).extracting(AchievementDefinition::getCriteriaType)
                .containsExactlyInAnyOrder(AchievementCriteriaType.ASSESSMENT_PERFECT_COUNT,
                        AchievementCriteriaType.FIRST_SUBTOPIC_COMPLETED);
    }

    @Test
    void writesCurrentAssessmentCriteriaName() {
        AchievementDefinition achievement = entityManager.persistAndFlush(new AchievementDefinition(
                "ASSESSMENT_ROOKIE", "Assessment Rookie", "Perfect assessment", "A", 15,
                AchievementCriteriaType.ASSESSMENT_PERFECT_COUNT, 1));

        Object storedValue = entityManager.getEntityManager().createNativeQuery(
                "SELECT criteria_type FROM achievement_definition WHERE id = :id")
                .setParameter("id", achievement.getId()).getSingleResult();

        assertThat(storedValue).isEqualTo("ASSESSMENT_PERFECT_COUNT");
    }
}
