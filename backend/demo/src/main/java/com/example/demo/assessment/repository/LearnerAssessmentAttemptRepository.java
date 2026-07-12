package com.example.demo.assessment.repository;

import com.example.demo.assessment.entity.LearnerAssessmentAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerAssessmentAttemptRepository extends JpaRepository<LearnerAssessmentAttempt, Long> {

    Optional<LearnerAssessmentAttempt> findByUserUserIdAndAssessmentId(UUID userId, Long assessmentId);

    boolean existsByUserUserIdAndAssessmentId(UUID userId, Long assessmentId);
}
