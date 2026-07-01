package com.example.demo.course.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.course.entity.CourseReview;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    List<CourseReview> findByCourseIdOrderByCreatedAtDescIdDesc(Long courseId);

    void deleteByCourseId(Long courseId);
}
