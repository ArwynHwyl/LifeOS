package com.example.demo.repository.course;

import com.example.demo.entity.course.DocumentSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSourceRepository extends JpaRepository<DocumentSource, Long> {

    List<DocumentSource> findByCourseIdOrderByCreatedAtDescIdDesc(Long courseId);
}
