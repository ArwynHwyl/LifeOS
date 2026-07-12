package com.example.demo.assessment.repository;

import com.example.demo.assessment.entity.AssessmentQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {

    List<AssessmentQuestion> findByAssessmentIdOrderBySortOrderAscIdAsc(Long assessmentId);

    long countByAssessmentId(Long assessmentId);
}
