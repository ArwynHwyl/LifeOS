package com.example.demo.controller;

import com.example.demo.dto.course.CourseReviewDetailDto;
import com.example.demo.dto.course.CourseReviewDto;
import com.example.demo.dto.course.CourseSummaryDto;
import com.example.demo.dto.course.ReviewDecisionRequest;
import com.example.demo.entity.User;
import com.example.demo.service.course.CourseReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherCourseReviewController {

    private final CourseReviewService courseReviewService;

    public TeacherCourseReviewController(CourseReviewService courseReviewService) {
        this.courseReviewService = courseReviewService;
    }

    @GetMapping("/course-reviews")
    public List<CourseSummaryDto> listPendingReviews(@AuthenticationPrincipal User user) {
        return courseReviewService.listPendingReviews(CurrentUser.id(user));
    }

    @GetMapping("/course-reviews/{courseId}")
    public CourseReviewDetailDto getCourseForReview(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return courseReviewService.getCourseForReview(CurrentUser.id(user), courseId);
    }

    @PostMapping("/course-reviews/{courseId}/approve")
    public CourseReviewDto approve(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @Valid @RequestBody(required = false) ReviewDecisionRequest request
    ) {
        return courseReviewService.approve(CurrentUser.id(user), courseId, request);
    }

    @PostMapping("/course-reviews/{courseId}/request-revision")
    public CourseReviewDto requestRevision(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @Valid @RequestBody(required = false) ReviewDecisionRequest request
    ) {
        return courseReviewService.requestRevision(CurrentUser.id(user), courseId, request);
    }
}
