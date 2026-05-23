package com.example.demo.repository.course;

import com.example.demo.entity.course.CourseModule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    List<CourseModule> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);
}
