package com.example.demo.course.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseStatus;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "modules")
    List<Course> findByTitleOrderByCreatedAtAsc(String title);

    List<Course> findByStatusOrderByUpdatedAtDesc(CourseStatus status);

}
