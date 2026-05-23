package com.example.demo.service.course;

import com.example.demo.dto.course.DocumentCompleteRequest;
import com.example.demo.dto.course.DocumentPreviewPageDto;
import com.example.demo.dto.course.DocumentPreviewResponse;
import com.example.demo.dto.course.DocumentSourceDto;
import com.example.demo.dto.course.DocumentUploadUrlRequest;
import com.example.demo.dto.course.DocumentUploadUrlResponse;
import com.example.demo.dto.course.DocumentUploadRequest;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.DocumentSource;
import com.example.demo.entity.User;
import com.example.demo.repository.course.CourseRepository;
import com.example.demo.repository.course.DocumentSourceRepository;
import com.example.demo.service.exception.ResourceNotFoundException;
import com.example.demo.service.exception.ValidationException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentSourceService {

    private static final long MAX_PDF_SIZE_BYTES = 250L * 1024L * 1024L;
    private static final int MAX_PREVIEW_PAGE_RANGE = 100;
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Duration READ_URL_TTL = Duration.ofMinutes(30);

    private final CourseRepository courseRepository;
    private final DocumentSourceRepository documentSourceRepository;
    private final UserAccessService userAccessService;
    private final CourseWorkflowGuard workflowGuard;
    private final CourseDtoMapper mapper;
    private final CourseInputValidator validator;
    private final PdfStorageService storageService;
    private final PdfDocumentService pdfDocumentService;

    public DocumentSourceService(
            CourseRepository courseRepository,
            DocumentSourceRepository documentSourceRepository,
            UserAccessService userAccessService,
            CourseWorkflowGuard workflowGuard,
            CourseDtoMapper mapper,
            CourseInputValidator validator,
            PdfStorageService storageService,
            PdfDocumentService pdfDocumentService
    ) {
        this.courseRepository = courseRepository;
        this.documentSourceRepository = documentSourceRepository;
        this.userAccessService = userAccessService;
        this.workflowGuard = workflowGuard;
        this.mapper = mapper;
        this.validator = validator;
        this.storageService = storageService;
        this.pdfDocumentService = pdfDocumentService;
    }

    @Transactional(readOnly = true)
    public List<DocumentSourceDto> listDocuments(UUID adminUserId, Long courseId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        ensureCourseExists(courseId);
        return documentSourceRepository.findByCourseIdOrderByCreatedAtDescIdDesc(courseId).stream()
                .map(mapper::toDocumentSourceDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentSourceDto getDocument(UUID adminUserId, Long documentId) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        return mapper.toDocumentSourceDto(findDocument(documentId));
    }

    @Transactional
    public DocumentUploadUrlResponse requestUploadUrl(UUID adminUserId, Long courseId, DocumentUploadUrlRequest request) {
        requireRequest(request);
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        Course course = findCourse(courseId);
        workflowGuard.prepareForAdminEdit(course);
        validatePdf(request.fileName(), request.fileType(), request.fileSizeBytes());

        String storagePath = buildStoragePath(course.getId(), request.fileName());
        PdfStorageService.PresignedUpload upload = storageService.presignUpload(
                storagePath,
                "application/pdf",
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
    public DocumentSourceDto completeUpload(UUID adminUserId, Long courseId, DocumentCompleteRequest request) {
        requireRequest(request);
        User admin = userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        Course course = findCourse(courseId);
        workflowGuard.prepareForAdminEdit(course);
        validatePdf(request.fileName(), request.fileType(), request.fileSizeBytes());
        String storagePath = validator.requiredText(request.storagePath(), "storagePath", 1_000);
        validateStoragePath(course.getId(), storagePath);

        PdfStorageService.ObjectMetadata metadata = storageService.metadata(storagePath)
                .orElseThrow(() -> new ResourceNotFoundException("Uploaded PDF was not found in storage"));
        if (metadata.contentLength() == null || metadata.contentLength() < 1) {
            throw new ValidationException("Uploaded PDF is empty");
        }
        if (metadata.contentLength() > MAX_PDF_SIZE_BYTES) {
            throw new ValidationException("PDF source documents must be 250MB or smaller");
        }
        if (!metadata.contentLength().equals(request.fileSizeBytes())) {
            throw new ValidationException("Uploaded PDF size does not match the completed upload request");
        }

        int pageCount;
        try (InputStream pdf = storageService.download(storagePath)) {
            pageCount = pdfDocumentService.countPages(pdf);
        } catch (java.io.IOException ex) {
            throw new ValidationException("Unable to read uploaded PDF from storage");
        }

        DocumentSource documentSource = new DocumentSource(
                validator.requiredText(request.fileName(), "fileName", 255),
                validator.requiredText(request.displayName(), "displayName", 255),
                "application/pdf",
                request.fileSizeBytes(),
                storagePath,
                admin,
                pageCount
        );
        course.addDocumentSource(documentSource);
        return mapper.toDocumentSourceDto(documentSourceRepository.save(documentSource));
    }

    @Transactional
    public DocumentSourceDto registerDocument(UUID adminUserId, Long courseId, DocumentUploadRequest request) {
        requireRequest(request);
        return completeUpload(adminUserId, courseId, new DocumentCompleteRequest(
                request.fileName(),
                request.displayName(),
                request.fileType(),
                request.fileSizeBytes(),
                request.storagePath()
        ));
    }

    @Transactional(readOnly = true)
    public DocumentPreviewResponse previewDocument(UUID adminUserId, Long documentId, Integer pageStart, Integer pageEnd) {
        userAccessService.requireAdmin(validator.requiredUserId(adminUserId));
        DocumentSource document = findDocument(documentId);
        Integer pageCount = document.getPageCount();
        if (pageCount == null) {
            throw new ValidationException("Document page count is unavailable");
        }
        int normalizedStart = pageStart == null ? 1 : pageStart;
        int normalizedEnd = pageEnd == null ? Math.min(10, pageCount) : pageEnd;
        validator.validateRequiredPageRange(normalizedStart, normalizedEnd, pageCount);
        if (normalizedEnd - normalizedStart + 1 > MAX_PREVIEW_PAGE_RANGE) {
            throw new ValidationException("Preview range cannot exceed 100 pages");
        }

        List<DocumentPreviewPageDto> pages;
        try (InputStream pdf = storageService.download(document.getStoragePath())) {
            pages = pdfDocumentService.extractPages(pdf, normalizedStart, normalizedEnd).stream()
                    .map(page -> new DocumentPreviewPageDto(page.pageNumber(), page.text()))
                    .toList();
        } catch (java.io.IOException ex) {
            throw new ValidationException("Unable to read uploaded PDF from storage");
        }

        PdfStorageService.PresignedRead read = storageService.presignRead(document.getStoragePath(), READ_URL_TTL);
        return new DocumentPreviewResponse(
                document.getId(),
                read.fileUrl(),
                read.expiresAt(),
                normalizedStart,
                normalizedEnd,
                pageCount,
                pages
        );
    }

    private void validatePdf(String rawFileName, String rawFileType, Long rawFileSizeBytes) {
        String fileName = validator.requiredText(rawFileName, "fileName", 255).toLowerCase();
        String fileType = validator.requiredText(rawFileType, "fileType", 100).toLowerCase();
        long fileSizeBytes = validator.requiredPositiveLong(rawFileSizeBytes, "fileSizeBytes");
        if (!fileName.endsWith(".pdf") || !fileType.equals("application/pdf")) {
            throw new ValidationException("Only PDF source documents are supported for the MVP");
        }
        if (fileSizeBytes > MAX_PDF_SIZE_BYTES) {
            throw new ValidationException("PDF source documents must be 250MB or smaller");
        }
    }

    private String buildStoragePath(Long courseId, String fileName) {
        return "courses/" + courseId + "/documents/" + UUID.randomUUID() + "/" + safeFileName(fileName);
    }

    private String safeFileName(String fileName) {
        String safe = validator.requiredText(fileName, "fileName", 255)
                .replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "source.pdf" : safe;
    }

    private void validateStoragePath(Long courseId, String storagePath) {
        String prefix = "courses/" + courseId + "/documents/";
        if (!storagePath.startsWith(prefix) || storagePath.contains("..")) {
            throw new ValidationException("storagePath is not valid for this course");
        }
    }

    private void ensureCourseExists(Long courseId) {
        if (!courseRepository.existsById(validator.requiredId(courseId, "courseId"))) {
            throw new ResourceNotFoundException("Course not found: " + courseId);
        }
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(validator.requiredId(courseId, "courseId"))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
    }

    private DocumentSource findDocument(Long documentId) {
        return documentSourceRepository.findById(validator.requiredId(documentId, "documentId"))
                .orElseThrow(() -> new ResourceNotFoundException("Document source not found: " + documentId));
    }

    private void requireRequest(DocumentUploadRequest request) {
        if (request == null) {
            throw new ValidationException("Document upload request is required");
        }
    }

    private void requireRequest(DocumentUploadUrlRequest request) {
        if (request == null) {
            throw new ValidationException("Document upload URL request is required");
        }
    }

    private void requireRequest(DocumentCompleteRequest request) {
        if (request == null) {
            throw new ValidationException("Document completion request is required");
        }
    }
}
