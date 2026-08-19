package com.example.demo.assessment.repository;

import com.example.demo.assessment.entity.LearnerAssessmentAttempt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearnerAssessmentAttemptRepository extends JpaRepository<LearnerAssessmentAttempt, Long> {

    Optional<LearnerAssessmentAttempt> findByUserUserIdAndAssessmentId(UUID userId, Long assessmentId);

    boolean existsByUserUserIdAndAssessmentId(UUID userId, Long assessmentId);

    @Query("select count(a) from LearnerAssessmentAttempt a where a.user.userId = :userId and a.score = a.totalQuestions")
    long countPerfectByUserId(@Param("userId") UUID userId);
}
