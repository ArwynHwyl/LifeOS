package com.example.demo.course.mapper;

import com.example.demo.course.service.document.PdfStorageService;
import com.example.demo.course.service.interactive.LessonHtmlService;

import com.example.demo.course.dto.generation.response.AiGenerationLogDto;
import com.example.demo.course.dto.management.response.CourseDetailDto;
import com.example.demo.course.entity.AiGenerationLog;
import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseModule;
import com.example.demo.course.entity.CourseReview;
import com.example.demo.course.entity.CourseReviewComment;
import com.example.demo.course.entity.DocumentSource;
import com.example.demo.course.entity.InteractionType;
import com.example.demo.course.entity.SubTopic;
import com.example.demo.course.entity.SubTopicAsset;
import com.example.demo.course.dto.review.response.CourseReviewCommentDto;
import com.example.demo.course.dto.review.response.CourseReviewDto;
import com.example.demo.course.dto.management.response.CourseSummaryDto;
import com.example.demo.course.dto.document.response.DocumentSourceDto;
import com.example.demo.course.dto.management.response.ModuleDto;
import com.example.demo.course.dto.interactive.response.PublishedCourseDetailDto;
import com.example.demo.course.dto.interactive.response.PublishedCourseSummaryDto;
import com.example.demo.course.dto.interactive.response.InteractiveProgressDto;
import com.example.demo.course.dto.interactive.response.PublishedModuleDto;
import com.example.demo.course.dto.interactive.response.PublishedSubTopicDto;
import com.example.demo.course.dto.management.response.SubTopicDto;
import com.example.demo.course.dto.interactive.response.SubTopicAssetDto;
import com.example.demo.user.entity.User;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class CourseDtoMapper {

    private static final Duration ASSET_READ_URL_TTL = Duration.ofMinutes(30);

    private final LessonHtmlService lessonHtmlService;
    private final PdfStorageService storageService;

    public CourseDtoMapper(LessonHtmlService lessonHtmlService, PdfStorageService storageService) {
        this.lessonHtmlService = lessonHtmlService;
        this.storageService = storageService;
    }

    public CourseSummaryDto toSummaryDto(Course course) {
        User approvedBy = course.getApprovedBy();
        return new CourseSummaryDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverId(),
                course.getStatus(),
                course.getCreatedBy().getUserId(),
                displayName(course.getCreatedBy()),
                approvedBy == null ? null : approvedBy.getUserId(),
                approvedBy == null ? null : displayName(approvedBy),
                course.getPublishedAt(),
                course.getModules().size(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }

    public CourseDetailDto toDetailDto(Course course) {
        User approvedBy = course.getApprovedBy();
        return new CourseDetailDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverId(),
                course.getStatus(),
                course.getCreatedBy().getUserId(),
                displayName(course.getCreatedBy()),
                approvedBy == null ? null : approvedBy.getUserId(),
                approvedBy == null ? null : displayName(approvedBy),
                course.getPublishedAt(),
                course.getCreatedAt(),
                course.getUpdatedAt(),
                course.getModules().stream()
                        .sorted(Comparator.comparing(CourseModule::getSortOrder).thenComparing(CourseModule::getId))
                        .map(this::toModuleDto)
                        .toList(),
                course.getDocumentSources().stream()
                        .map(this::toDocumentSourceDto)
                        .toList()
        );
    }

    public ModuleDto toModuleDto(CourseModule module) {
        return new ModuleDto(
                module.getId(),
                module.getCourse().getId(),
                module.getTitle(),
                module.getDescription(),
                module.getSortOrder(),
                module.getContentDepth(),
                module.getInteractionType(),
                module.getInteractionPrompt(),
                module.getInteractionConfig(),
                module.getCreatedAt(),
                module.getUpdatedAt(),
                module.getSubTopics().stream()
                        .sorted(Comparator.comparing(SubTopic::getSortOrder).thenComparing(SubTopic::getId))
                        .map(this::toSubTopicDto)
                        .toList()
        );
    }

    public SubTopicDto toSubTopicDto(SubTopic subTopic) {
        return new SubTopicDto(
                subTopic.getId(),
                subTopic.getModule().getId(),
                subTopic.getTitle(),
                subTopic.getContent(),
                lessonHtmlService.contentHtmlWithAssetUrls(
                        subTopic.getContent(),
                        subTopic.getAssets(),
                        asset -> storageService.presignRead(asset.getStoragePath(), ASSET_READ_URL_TTL)
                ),
                subTopic.getMascotPrompt(),
                subTopic.getAssets().stream()
                        .map(this::toSubTopicAssetDto)
                        .toList(),
                subTopic.getSortOrder(),
                subTopic.getSourceType(),
                subTopic.getPageStart(),
                subTopic.getPageEnd(),
                subTopic.getInteractionType(),
                subTopic.getInteractionPrompt(),
                subTopic.getInteractionConfig(),
                subTopic.getCreatedAt(),
                subTopic.getUpdatedAt()
        );
    }

    public SubTopicAssetDto toSubTopicAssetDto(SubTopicAsset asset) {
        PdfStorageService.PresignedRead read = storageService.presignRead(asset.getStoragePath(), ASSET_READ_URL_TTL);
        return new SubTopicAssetDto(
                asset.getId(),
                asset.getSubTopic().getId(),
                asset.getFileName(),
                asset.getFileType(),
                asset.getFileSizeBytes(),
                asset.getAltText(),
                asset.getStoragePath(),
                read.fileUrl(),
                read.expiresAt(),
                asset.getCreatedAt()
        );
    }

    public DocumentSourceDto toDocumentSourceDto(DocumentSource documentSource) {
        return new DocumentSourceDto(
                documentSource.getId(),
                documentSource.getCourse().getId(),
                documentSource.getFileName(),
                documentSource.getDisplayName(),
                documentSource.getFileType(),
                documentSource.getFileSizeBytes(),
                documentSource.getStoragePath(),
                documentSource.getUploadedBy().getUserId(),
                displayName(documentSource.getUploadedBy()),
                documentSource.getPageCount(),
                documentSource.getCreatedAt(),
                documentSource.getUpdatedAt()
        );
    }

    public AiGenerationLogDto toAiGenerationLogDto(AiGenerationLog log) {
        return new AiGenerationLogDto(
                log.getId(),
                log.getCourse().getId(),
                log.getModule() == null ? null : log.getModule().getId(),
                log.getType(),
                log.getRequestedBy().getUserId(),
                log.getDocumentSource().getId(),
                log.getPageStart(),
                log.getPageEnd(),
                log.getRequirements(),
                log.getPrompt(),
                log.getStatus(),
                log.getRawResponse(),
                log.getErrorMessage(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }

    public CourseReviewDto toCourseReviewDto(CourseReview review) {
        return new CourseReviewDto(
                review.getId(),
                review.getCourse().getId(),
                review.getReviewer().getUserId(),
                displayName(review.getReviewer()),
                review.getDecision(),
                review.getFeedback(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getComments().stream()
                        .map(this::toCourseReviewCommentDto)
                        .toList()
        );
    }

    public CourseReviewCommentDto toCourseReviewCommentDto(CourseReviewComment comment) {
        CourseModule module = comment.getModule();
        SubTopic subTopic = comment.getSubTopic();
        return new CourseReviewCommentDto(
                comment.getId(),
                module == null ? null : module.getId(),
                subTopic == null ? null : subTopic.getId(),
                comment.getFeedback(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.isResolved(),
                comment.getResolvedAt()
        );
    }

    public List<CourseSummaryDto> toSummaryDtos(List<Course> courses) {
        return courses.stream().map(this::toSummaryDto).toList();
    }

    public PublishedCourseSummaryDto toPublishedSummaryDto(Course course) {
        return new PublishedCourseSummaryDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverId(),
                course.getPublishedAt()
        );
    }

    public PublishedCourseDetailDto toPublishedDetailDto(Course course) {
        return toPublishedDetailDto(course, Map.of());
    }

    public PublishedCourseDetailDto toPublishedDetailDto(Course course, Map<Long, InteractiveProgressDto> progressBySubTopicId) {
        return new PublishedCourseDetailDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverId(),
                course.getPublishedAt(),
                course.getModules().stream()
                        .sorted(Comparator.comparing(CourseModule::getSortOrder).thenComparing(CourseModule::getId))
                        .map(module -> toPublishedModuleDto(module, progressBySubTopicId))
                        .toList()
        );
    }

    public List<PublishedCourseSummaryDto> toPublishedSummaryDtos(List<Course> courses) {
        return courses.stream().map(this::toPublishedSummaryDto).toList();
    }

    private PublishedModuleDto toPublishedModuleDto(CourseModule module) {
        return toPublishedModuleDto(module, Map.of());
    }

    private PublishedModuleDto toPublishedModuleDto(CourseModule module, Map<Long, InteractiveProgressDto> progressBySubTopicId) {
        return new PublishedModuleDto(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                module.getSortOrder(),
                module.getInteractionType(),
                module.getInteractionPrompt(),
                module.getInteractionConfig(),
                module.getSubTopics().stream()
                        .sorted(Comparator.comparing(SubTopic::getSortOrder).thenComparing(SubTopic::getId))
                        .map(subTopic -> toPublishedSubTopicDto(subTopic, progressBySubTopicId))
                        .toList()
        );
    }

    private PublishedSubTopicDto toPublishedSubTopicDto(SubTopic subTopic) {
        return toPublishedSubTopicDto(subTopic, Map.of());
    }

    private PublishedSubTopicDto toPublishedSubTopicDto(SubTopic subTopic, Map<Long, InteractiveProgressDto> progressBySubTopicId) {
        return new PublishedSubTopicDto(
                subTopic.getId(),
                subTopic.getTitle(),
                subTopic.getContent(),
                lessonHtmlService.contentHtmlWithAssetUrls(
                        subTopic.getContent(),
                        subTopic.getAssets(),
                        asset -> storageService.presignRead(asset.getStoragePath(), ASSET_READ_URL_TTL)
                ),
                subTopic.getMascotPrompt(),
                subTopic.getSortOrder(),
                subTopic.getInteractionType(),
                subTopic.getInteractionPrompt(),
                subTopic.getInteractionConfig(),
                progressBySubTopicId.get(subTopic.getId())
        );
    }

    private String displayName(User user) {
        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return fullName.isBlank() ? user.getUsername() : fullName;
    }
}
