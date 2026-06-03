package com.example.demo.controller;

import com.example.demo.dto.course.InteractiveProgressDto;
import com.example.demo.dto.course.InteractiveProgressUpdateRequest;
import com.example.demo.dto.course.PublishedCourseDetailDto;
import com.example.demo.dto.course.PublishedCourseSummaryDto;
import com.example.demo.entity.User;
import com.example.demo.service.course.LearnerCourseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public PublishedCourseDetailDto getPublishedCourse(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return learnerCourseService.getPublishedCourse(CurrentUser.id(user), courseId);
    }

    @PutMapping("/{courseId}/subtopics/{subTopicId}/interactive-progress")
    public InteractiveProgressDto updateInteractiveProgress(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @PathVariable Long subTopicId,
            @Valid @RequestBody InteractiveProgressUpdateRequest request
    ) {
        return learnerCourseService.updateInteractiveProgress(CurrentUser.id(user), courseId, subTopicId, request);
    }
}
