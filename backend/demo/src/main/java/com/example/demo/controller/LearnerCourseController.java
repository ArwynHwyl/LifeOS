package com.example.demo.controller;

import com.example.demo.dto.course.PublishedCourseDetailDto;
import com.example.demo.dto.course.PublishedCourseSummaryDto;
import com.example.demo.service.course.LearnerCourseService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learner/courses")
@PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'ADMIN')")
public class LearnerCourseController {

    private final LearnerCourseService learnerCourseService;

    public LearnerCourseController(LearnerCourseService learnerCourseService) {
        this.learnerCourseService = learnerCourseService;
    }

    @GetMapping
    public List<PublishedCourseSummaryDto> listPublishedCourses() {
        return learnerCourseService.listPublishedCourses();
    }

    @GetMapping("/{courseId}")
    public PublishedCourseDetailDto getPublishedCourse(@PathVariable Long courseId) {
        return learnerCourseService.getPublishedCourse(courseId);
    }
}
