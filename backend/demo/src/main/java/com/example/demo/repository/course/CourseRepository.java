package com.example.demo.repository.course;

import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrderByCreatedAtDesc();

    List<Course> findByStatusOrderByUpdatedAtDesc(CourseStatus status);

}
