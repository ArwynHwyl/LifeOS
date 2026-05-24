package com.example.demo.repository.course;

import com.example.demo.entity.course.AiGenerationLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {

    List<AiGenerationLog> findByCourseIdOrderByCreatedAtDescIdDesc(Long courseId);

    void deleteByCourseId(Long courseId);
}
