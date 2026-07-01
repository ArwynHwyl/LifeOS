package com.example.demo.course.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.course.entity.DocumentSource;

public interface DocumentSourceRepository extends JpaRepository<DocumentSource, Long> {

    List<DocumentSource> findByCourseIdOrderByCreatedAtDescIdDesc(Long courseId);
}
