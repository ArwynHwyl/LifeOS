package com.example.demo.service.course;

import com.example.demo.dto.course.AiGenerationLogDto;
import com.example.demo.dto.course.CourseDetailDto;
import com.example.demo.dto.course.CourseReviewCommentDto;
import com.example.demo.dto.course.CourseReviewDto;
import com.example.demo.dto.course.CourseSummaryDto;
import com.example.demo.dto.course.DocumentSourceDto;
import com.example.demo.dto.course.ModuleDto;
import com.example.demo.dto.course.PublishedCourseDetailDto;
import com.example.demo.dto.course.PublishedCourseSummaryDto;
import com.example.demo.dto.course.InteractiveProgressDto;
import com.example.demo.dto.course.PublishedModuleDto;
import com.example.demo.dto.course.PublishedSubTopicDto;
import com.example.demo.dto.course.SubTopicDto;
import com.example.demo.dto.course.SubTopicAssetDto;
import com.example.demo.entity.User;
import com.example.demo.entity.course.AiGenerationLog;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.InteractionType;
import com.example.demo.entity.course.CourseReview;
import com.example.demo.entity.course.CourseReviewComment;
import com.example.demo.entity.course.DocumentSource;
import com.example.demo.entity.course.SubTopic;
import com.example.demo.entity.course.SubTopicAsset;
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
                comment.getUpdatedAt()
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
                subTopic.getInteractionType() == InteractionType.NONE ? null : progressBySubTopicId.get(subTopic.getId())
        );
    }

    private String displayName(User user) {
        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return fullName.isBlank() ? user.getUsername() : fullName;
    }
}
