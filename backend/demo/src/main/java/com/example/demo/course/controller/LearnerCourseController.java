package com.example.demo.course.controller;

import com.example.demo.course.dto.interactive.request.InteractiveAttemptRequest;
import com.example.demo.course.dto.interactive.response.InteractiveAttemptResponse;
import com.example.demo.course.dto.interactive.response.InteractiveProgressDto;
import com.example.demo.course.dto.interactive.request.InteractiveProgressUpdateRequest;
import com.example.demo.course.dto.interactive.request.LogicAttemptRequest;
import com.example.demo.course.dto.interactive.response.LogicAttemptResponse;
import com.example.demo.course.dto.interactive.response.PublishedCourseDetailDto;
import com.example.demo.course.dto.interactive.response.PublishedCourseSummaryDto;
import com.example.demo.user.entity.User;
import com.example.demo.course.service.learner.LearnerCourseService;
import com.example.demo.shared.security.CurrentUser;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/{courseId}/subtopics/{subTopicId}/logic-attempts")
    public LogicAttemptResponse submitLogicAttempt(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @PathVariable Long subTopicId,
            @Valid @RequestBody LogicAttemptRequest request
    ) {
        return learnerCourseService.submitLogicAttempt(CurrentUser.id(user), courseId, subTopicId, request);
    }

    @PostMapping("/{courseId}/subtopics/{subTopicId}/interactive-attempts")
    public InteractiveAttemptResponse submitInteractiveAttempt(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @PathVariable Long subTopicId,
            @Valid @RequestBody InteractiveAttemptRequest request
    ) {
        return learnerCourseService.submitInteractiveAttempt(CurrentUser.id(user), courseId, subTopicId, request);
    }
}
