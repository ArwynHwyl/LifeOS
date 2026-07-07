package com.example.demo.gamification.repository;

import com.example.demo.gamification.entity.AchievementDefinition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, Long> {

    Optional<AchievementDefinition> findByCode(String code);

    boolean existsByCode(String code);
}
