package com.example.demo.course.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.course.entity.CourseReviewComment;

public interface CourseReviewCommentRepository extends JpaRepository<CourseReviewComment, Long> {

    List<CourseReviewComment> findByReviewIdOrderByIdAsc(Long reviewId);
}
