package com.example.demo.repository.course;

import com.example.demo.entity.course.CourseReviewComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseReviewCommentRepository extends JpaRepository<CourseReviewComment, Long> {

    List<CourseReviewComment> findByReviewIdOrderByIdAsc(Long reviewId);
}
