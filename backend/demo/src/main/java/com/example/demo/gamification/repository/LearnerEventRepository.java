package com.example.demo.gamification.repository;

import com.example.demo.gamification.entity.LearnerEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerEventRepository extends JpaRepository<LearnerEvent, Long> {

    List<LearnerEvent> findByUserUserIdAndAcknowledgedAtIsNullOrderByCreatedAtAsc(UUID userId);
}
