package com.example.demo.service.course;

import com.example.demo.dto.course.DocumentUploadUrlResponse;
import com.example.demo.dto.course.SubTopicAssetDto;
import com.example.demo.dto.course.SubTopicImageCompleteRequest;
import com.example.demo.dto.course.SubTopicImageUploadUrlRequest;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.SubTopic;
import com.example.demo.entity.course.SubTopicAsset;
import com.example.demo.repository.course.SubTopicAssetRepository;
import com.example.demo.repository.course.SubTopicRepository;
import com.example.demo.service.exception.ResourceNotFoundException;
import com.example.demo.service.exception.ValidationException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubTopicImageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final SubTopicRepository subTopicRepository;
    private final SubTopicAssetRepository subTopicAssetRepository;
    private final UserAccessService userAccessService;
    private final CourseWorkflowGuard workflowGuard;
    private final CourseInputValidator validator;
    private final PdfStorageService storageService;
    private final CourseDtoMapper mapper;

    public SubTopicImageService(
            SubTopicRepository subTopicRepository,
            SubTopicAssetRepository subTopicAssetRepository,
            UserAccessService userAccessService,
            CourseWorkflowGuard workflowGuard,
            CourseInputValidator validator,
            PdfStorageService storageService,
            CourseDtoMapper mapper
    ) {
        this.subTopicRepository = subTopicRepository;
        this.subTopicAssetRepository = subTopicAssetRepository;
        this.userAccessService = userAccessService;
        this.workflowGuard = workflowGuard;
        this.validator = validator;
        this.storageService = storageService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SubTopicAssetDto> listAssets(UUID adminUserId, Long subTopicId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        findSubTopic(subTopicId);
        return subTopicAssetRepository.findBySubTopicIdOrderByCreatedAtAscIdAsc(subTopicId).stream()
                .map(mapper::toSubTopicAssetDto)
                .toList();
    }

    @Transactional
    public DocumentUploadUrlResponse requestUploadUrl(
            UUID adminUserId,
            Long subTopicId,
            SubTopicImageUploadUrlRequest request
    ) {
        requireRequest(request);
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        SubTopic subTopic = findSubTopic(subTopicId);
        Course course = subTopic.getModule().getCourse();
        workflowGuard.prepareForAdminEdit(course);
        validateImage(request.fileName(), request.fileType(), request.fileSizeBytes());

        String storagePath = buildStoragePath(course.getId(), subTopic.getId(), request.fileName());
        PdfStorageService.PresignedUpload upload = storageService.presignUpload(
                storagePath,
                request.fileType().toLowerCase(),
                UPLOAD_URL_TTL
        );
        return new DocumentUploadUrlResponse(
                upload.uploadUrl(),
                upload.storagePath(),
                upload.expiresAt(),
                upload.method(),
                upload.headers()
        );
    }

    @Transactional
    public SubTopicAssetDto completeUpload(UUID adminUserId, Long subTopicId, SubTopicImageCompleteRequest request) {
        requireRequest(request);
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        SubTopic subTopic = findSubTopic(subTopicId);
        Course course = subTopic.getModule().getCourse();
        workflowGuard.prepareForAdminEdit(course);
        validateImage(request.fileName(), request.fileType(), request.fileSizeBytes());
        String storagePath = validator.requiredText(request.storagePath(), "storagePath", 1_000);
        validateStoragePath(course.getId(), subTopic.getId(), storagePath);

        PdfStorageService.ObjectMetadata metadata = storageService.metadata(storagePath)
                .orElseThrow(() -> new ResourceNotFoundException("Uploaded image was not found in storage"));
        if (metadata.contentLength() == null || metadata.contentLength() < 1) {
            throw new ValidationException("Uploaded image is empty");
        }
        if (metadata.contentLength() > MAX_IMAGE_SIZE_BYTES) {
            throw new ValidationException("Subtopic images must be 10MB or smaller");
        }
        if (!metadata.contentLength().equals(request.fileSizeBytes())) {
            throw new ValidationException("Uploaded image size does not match the completed upload request");
        }
        if (metadata.contentType() != null && !ALLOWED_IMAGE_TYPES.contains(metadata.contentType().toLowerCase())) {
            throw new ValidationException("Uploaded image content type is not supported");
        }

        SubTopicAsset asset = new SubTopicAsset(
                storagePath,
                validator.requiredText(request.fileName(), "fileName", 255),
                request.fileType().toLowerCase(),
                request.fileSizeBytes(),
                validator.optionalText(request.altText(), "altText", 500)
        );
        subTopic.addAsset(asset);
        return mapper.toSubTopicAssetDto(subTopicAssetRepository.save(asset));
    }

    private void validateImage(String rawFileName, String rawFileType, Long rawFileSizeBytes) {
        String fileName = validator.requiredText(rawFileName, "fileName", 255).toLowerCase();
        String fileType = validator.requiredText(rawFileType, "fileType", 100).toLowerCase();
        long fileSizeBytes = validator.requiredPositiveLong(rawFileSizeBytes, "fileSizeBytes");
        if (!ALLOWED_IMAGE_TYPES.contains(fileType)) {
            throw new ValidationException("Only PNG, JPEG, and WebP images are supported");
        }
        if (fileType.equals("image/png") && !fileName.endsWith(".png")) {
            throw new ValidationException("PNG images must use a .png file extension");
        }
        if (fileType.equals("image/jpeg") && !(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))) {
            throw new ValidationException("JPEG images must use a .jpg or .jpeg file extension");
        }
        if (fileType.equals("image/webp") && !fileName.endsWith(".webp")) {
            throw new ValidationException("WebP images must use a .webp file extension");
        }
        if (fileSizeBytes > MAX_IMAGE_SIZE_BYTES) {
            throw new ValidationException("Subtopic images must be 10MB or smaller");
        }
    }

    private String buildStoragePath(Long courseId, Long subTopicId, String fileName) {
        return "courses/" + courseId + "/subtopics/" + subTopicId + "/images/"
                + UUID.randomUUID() + "-" + safeFileName(fileName);
    }

    private String safeFileName(String fileName) {
        String safe = validator.requiredText(fileName, "fileName", 255)
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "image" : safe;
    }

    private void validateStoragePath(Long courseId, Long subTopicId, String storagePath) {
        String prefix = "courses/" + courseId + "/subtopics/" + subTopicId + "/images/";
        if (!storagePath.startsWith(prefix) || storagePath.contains("..")) {
            throw new ValidationException("storagePath is not valid for this subtopic");
        }
    }

    private SubTopic findSubTopic(Long subTopicId) {
        return subTopicRepository.findById(validator.requiredId(subTopicId, "subTopicId"))
                .orElseThrow(() -> new ResourceNotFoundException("Sub-topic not found: " + subTopicId));
    }

    private void requireRequest(Object request) {
        if (request == null) {
            throw new ValidationException("Subtopic image request is required");
        }
    }
}
