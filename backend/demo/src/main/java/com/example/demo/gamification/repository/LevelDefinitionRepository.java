package com.example.demo.gamification.repository;

import com.example.demo.gamification.entity.LevelDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelDefinitionRepository extends JpaRepository<LevelDefinition, Integer> {

    List<LevelDefinition> findAllByOrderByLevelAsc();
}
