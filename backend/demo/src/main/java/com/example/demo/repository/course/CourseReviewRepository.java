package com.example.demo.repository.course;

import com.example.demo.entity.course.CourseReview;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    List<CourseReview> findByCourseIdOrderByCreatedAtDescIdDesc(Long courseId);

    void deleteByCourseId(Long courseId);
}
