package com.example.demo.assessment.controller;

import com.example.demo.assessment.dto.AssessmentDetailDto;
import com.example.demo.assessment.dto.AssessmentResultDto;
import com.example.demo.assessment.dto.AssessmentSummaryDto;
import com.example.demo.assessment.dto.SubmitAssessmentRequest;
import com.example.demo.assessment.service.AssessmentService;
import com.example.demo.shared.security.CurrentUser;
import com.example.demo.user.entity.User;
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
@RequestMapping("/api/v1/learner/assessments")
@PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'ADMIN')")
public class LearnerAssessmentController {

    private final AssessmentService assessmentService;

    public LearnerAssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public List<AssessmentSummaryDto> listAssessments(@AuthenticationPrincipal User user) {
        return assessmentService.listAssessments(CurrentUser.id(user));
    }

    @GetMapping("/{assessmentId}")
    public AssessmentDetailDto getAssessment(@AuthenticationPrincipal User user, @PathVariable Long assessmentId) {
        return assessmentService.getAssessmentDetail(CurrentUser.id(user), assessmentId);
    }

    @PostMapping("/{assessmentId}/submit")
    public AssessmentResultDto submitAssessment(
            @AuthenticationPrincipal User user,
            @PathVariable Long assessmentId,
            @Valid @RequestBody SubmitAssessmentRequest request
    ) {
        return assessmentService.submitAssessment(CurrentUser.id(user), assessmentId, request);
    }
}
