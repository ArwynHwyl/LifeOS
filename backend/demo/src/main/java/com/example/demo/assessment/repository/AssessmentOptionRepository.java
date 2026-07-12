package com.example.demo.assessment.repository;

import com.example.demo.assessment.entity.AssessmentOption;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentOptionRepository extends JpaRepository<AssessmentOption, Long> {

    Optional<AssessmentOption> findByIdAndQuestionId(Long id, Long questionId);
}
