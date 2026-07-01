package com.example.demo.course.controller;

import com.example.demo.course.dto.generation.response.AiGenerationLogDto;
import com.example.demo.course.dto.management.response.CourseDetailDto;
import com.example.demo.course.dto.generation.request.AiGenerationRequest;
import com.example.demo.course.dto.management.request.CourseCreateRequest;
import com.example.demo.course.dto.generation.response.AiGenerationResultDto;
import com.example.demo.course.dto.review.response.CourseReviewDto;
import com.example.demo.course.dto.management.response.CourseSummaryDto;
import com.example.demo.course.dto.management.request.CourseUpdateRequest;
import com.example.demo.course.dto.document.request.DocumentCompleteRequest;
import com.example.demo.course.dto.document.response.DocumentPreviewResponse;
import com.example.demo.course.dto.document.response.DocumentSourceDto;
import com.example.demo.course.dto.document.request.DocumentUploadUrlRequest;
import com.example.demo.course.dto.document.response.DocumentUploadUrlResponse;
import com.example.demo.course.dto.interactive.response.InteractiveTemplateDto;
import com.example.demo.course.dto.document.request.DocumentUploadRequest;
import com.example.demo.course.dto.management.request.ModuleCreateRequest;
import com.example.demo.course.dto.management.response.ModuleDto;
import com.example.demo.course.dto.management.request.ModuleUpdateRequest;
import com.example.demo.course.dto.management.request.SubTopicCreateRequest;
import com.example.demo.course.dto.interactive.response.SubTopicAssetDto;
import com.example.demo.course.dto.interactive.request.SubTopicImageCompleteRequest;
import com.example.demo.course.dto.interactive.request.SubTopicImageUploadUrlRequest;
import com.example.demo.course.dto.management.response.SubTopicDto;
import com.example.demo.course.dto.management.request.SubTopicUpdateRequest;
import com.example.demo.user.entity.User;
import com.example.demo.course.service.generation.AiGenerationService;
import com.example.demo.course.service.management.CourseAdminService;
import com.example.demo.course.service.document.DocumentSourceService;
import com.example.demo.course.service.interactive.InteractiveConfigService;
import com.example.demo.course.service.interactive.SubTopicImageService;
import com.example.demo.shared.security.CurrentUser;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.demo.course.dto.review.response.CourseReviewCommentDto;
import com.example.demo.course.dto.review.request.ReviewCommentResolveRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final CourseAdminService courseAdminService;
    private final DocumentSourceService documentSourceService;
    private final AiGenerationService aiGenerationService;
    private final SubTopicImageService subTopicImageService;
    private final InteractiveConfigService interactiveConfigService;

    public AdminCourseController(
            CourseAdminService courseAdminService,
            DocumentSourceService documentSourceService,
            AiGenerationService aiGenerationService,
            SubTopicImageService subTopicImageService,
            InteractiveConfigService interactiveConfigService
    ) {
        this.courseAdminService = courseAdminService;
        this.documentSourceService = documentSourceService;
        this.aiGenerationService = aiGenerationService;
        this.subTopicImageService = subTopicImageService;
        this.interactiveConfigService = interactiveConfigService;
    }

    @GetMapping("/interactive-templates")
    public List<InteractiveTemplateDto> listInteractiveTemplates() {
        return interactiveConfigService.listTemplates();
    }

    @GetMapping("/courses")
    public List<CourseSummaryDto> listCourses(@AuthenticationPrincipal User user) {
        return courseAdminService.listCourses(CurrentUser.id(user));
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseDetailDto> createCourse(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseAdminService.createCourse(CurrentUser.id(user), request));
    }

    @GetMapping("/courses/{courseId}")
    public CourseDetailDto getCourse(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return courseAdminService.getCourse(CurrentUser.id(user), courseId);
    }

    @GetMapping("/courses/{courseId}/reviews")
    public List<CourseReviewDto> getCourseReviews(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return courseAdminService.getCourseReviews(CurrentUser.id(user), courseId);
    }

    @PutMapping("/courses/{courseId}")
    public CourseDetailDto updateCourse(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        return courseAdminService.updateCourse(CurrentUser.id(user), courseId, request);
    }

    @DeleteMapping("/courses/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        courseAdminService.deleteCourse(CurrentUser.id(user), courseId);
    }

    @PostMapping("/courses/{courseId}/submit-review")
    public CourseDetailDto submitForReview(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return courseAdminService.submitForReview(CurrentUser.id(user), courseId);
    }

    @PostMapping("/courses/{courseId}/publish")
    public CourseDetailDto publish(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return courseAdminService.publish(CurrentUser.id(user), courseId);
    }

    @PostMapping("/courses/{courseId}/modules")
    public ResponseEntity<ModuleDto> addModule(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @Valid @RequestBody ModuleCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseAdminService.addModule(CurrentUser.id(user), courseId, request));
    }

    @PutMapping("/modules/{moduleId}")
    public ModuleDto updateModule(
            @AuthenticationPrincipal User user,
            @PathVariable Long moduleId,
            @Valid @RequestBody ModuleUpdateRequest request
    ) {
        return courseAdminService.updateModule(CurrentUser.id(user), moduleId, request);
    }

    @DeleteMapping("/modules/{moduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModule(@AuthenticationPrincipal User user, @PathVariable Long moduleId) {
        courseAdminService.deleteModule(CurrentUser.id(user), moduleId);
    }

    @PostMapping("/modules/{moduleId}/subtopics")
    public ResponseEntity<SubTopicDto> addSubTopic(
            @AuthenticationPrincipal User user,
            @PathVariable Long moduleId,
            @Valid @RequestBody SubTopicCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseAdminService.addSubTopic(CurrentUser.id(user), moduleId, request));
    }

    @PutMapping("/subtopics/{subTopicId}")
    public SubTopicDto updateSubTopic(
            @AuthenticationPrincipal User user,
            @PathVariable Long subTopicId,
            @Valid @RequestBody SubTopicUpdateRequest request
    ) {
        return courseAdminService.updateSubTopic(CurrentUser.id(user), subTopicId, request);
    }

    @DeleteMapping("/subtopics/{subTopicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubTopic(@AuthenticationPrincipal User user, @PathVariable Long subTopicId) {
        courseAdminService.deleteSubTopic(CurrentUser.id(user), subTopicId);
    }

    @GetMapping("/subtopics/{subTopicId}/assets")
    public List<SubTopicAssetDto> listSubTopicAssets(
            @AuthenticationPrincipal User user,
            @PathVariable Long subTopicId
    ) {
        return subTopicImageService.listAssets(CurrentUser.id(user), subTopicId);
    }

    @PostMapping("/subtopics/{subTopicId}/images/upload-url")
    public DocumentUploadUrlResponse requestSubTopicImageUploadUrl(
            @AuthenticationPrincipal User user,
            @PathVariable Long subTopicId,
            @Valid @RequestBody SubTopicImageUploadUrlRequest request
    ) {
        return subTopicImageService.requestUploadUrl(CurrentUser.id(user), subTopicId, request);
    }

    @PostMapping("/subtopics/{subTopicId}/images/complete")
    public ResponseEntity<SubTopicAssetDto> completeSubTopicImageUpload(
            @AuthenticationPrincipal User user,
            @PathVariable Long subTopicId,
            @Valid @RequestBody SubTopicImageCompleteRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(subTopicImageService.completeUpload(CurrentUser.id(user), subTopicId, request));
    }

    @GetMapping("/courses/{courseId}/documents")
    public List<DocumentSourceDto> listDocuments(@AuthenticationPrincipal User user, @PathVariable Long courseId) {
        return documentSourceService.listDocuments(CurrentUser.id(user), courseId);
    }

    @PostMapping("/courses/{courseId}/documents")
    public ResponseEntity<DocumentSourceDto> registerDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @Valid @RequestBody DocumentUploadRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(documentSourceService.registerDocument(CurrentUser.id(user), courseId, request));
    }

    @PostMapping("/courses/{courseId}/documents/upload-url")
    public DocumentUploadUrlResponse requestDocumentUploadUrl(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @Valid @RequestBody DocumentUploadUrlRequest request
    ) {
        return documentSourceService.requestUploadUrl(CurrentUser.id(user), courseId, request);
    }

    @PostMapping("/courses/{courseId}/documents/complete")
    public ResponseEntity<DocumentSourceDto> completeDocumentUpload(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @Valid @RequestBody DocumentCompleteRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(documentSourceService.completeUpload(CurrentUser.id(user), courseId, request));
    }

    @GetMapping("/documents/{documentId}")
    public DocumentSourceDto getDocument(@AuthenticationPrincipal User user, @PathVariable Long documentId) {
        return documentSourceService.getDocument(CurrentUser.id(user), documentId);
    }

    @GetMapping("/documents/{documentId}/preview")
    public DocumentPreviewResponse previewDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "1") Integer pageStart,
            @RequestParam(defaultValue = "10") Integer pageEnd
    ) {
        return documentSourceService.previewDocument(CurrentUser.id(user), documentId, pageStart, pageEnd);
    }

    @PostMapping("/modules/{moduleId}/ai-generations")
    public ResponseEntity<AiGenerationResultDto> requestAiGeneration(
            @AuthenticationPrincipal User user,
            @PathVariable Long moduleId,
            @Valid @RequestBody AiGenerationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(aiGenerationService.generateModuleDraft(CurrentUser.id(user), moduleId, request));
    }

    @PostMapping("/courses/{courseId}/ai-outline-generations")
    public ResponseEntity<AiGenerationLogDto> requestCourseOutlineGeneration(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @Valid @RequestBody AiGenerationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(aiGenerationService.requestCourseOutlineGeneration(CurrentUser.id(user), courseId, request));
    }

    @GetMapping("/courses/{courseId}/ai-generations")
    public List<AiGenerationLogDto> listAiGenerationLogs(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId
    ) {
        return aiGenerationService.listLogs(CurrentUser.id(user), courseId);
    }

    @GetMapping("/ai-generations/{logId}")
    public AiGenerationLogDto getAiGenerationLog(@AuthenticationPrincipal User user, @PathVariable Long logId) {
        return aiGenerationService.getLog(CurrentUser.id(user), logId);
    }

    @PatchMapping("/courses/{courseId}/review-comments/{commentId}/resolved")
    public CourseReviewCommentDto setReviewCommentResolved(
            @AuthenticationPrincipal User user,
            @PathVariable Long courseId,
            @PathVariable Long commentId,
            @Valid @RequestBody ReviewCommentResolveRequest request
    ) {
        return courseAdminService.setReviewCommentResolved(
                CurrentUser.id(user),
                courseId,
                commentId,
                request.resolved()
        );
    }
}
