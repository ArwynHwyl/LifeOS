package com.example.demo.assessment.repository;

import com.example.demo.assessment.entity.Assessment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findAllByOrderBySortOrderAscIdAsc();
}
