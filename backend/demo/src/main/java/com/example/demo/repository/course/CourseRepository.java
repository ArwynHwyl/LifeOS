package com.example.demo.repository.course;

import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "modules")
    List<Course> findByTitleOrderByCreatedAtAsc(String title);

    List<Course> findByStatusOrderByUpdatedAtDesc(CourseStatus status);

}
