package com.example.demo.course.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.course.entity.AiGenerationLog;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {

    List<AiGenerationLog> findByCourseIdOrderByCreatedAtDescIdDesc(Long courseId);

    void deleteByCourseId(Long courseId);
}
