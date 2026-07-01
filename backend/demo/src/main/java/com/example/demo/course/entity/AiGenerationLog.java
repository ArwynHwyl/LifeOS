package com.example.demo.course.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
import com.example.demo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_generation_log")
public class AiGenerationLog extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private CourseModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiGenerationType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_source_id", nullable = false)
    private DocumentSource documentSource;

    @Column(nullable = false)
    private Integer pageStart;

    @Column(nullable = false)
    private Integer pageEnd;

    @Column(columnDefinition = "text")
    private String requirements;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiGenerationStatus status;

    @Column(columnDefinition = "text")
    private String rawResponse;

    @Column(columnDefinition = "text")
    private String errorMessage;

    protected AiGenerationLog() {
    }

    public AiGenerationLog(
            Course course,
            CourseModule module,
            User requestedBy,
            DocumentSource documentSource,
            Integer pageStart,
            Integer pageEnd,
            String requirements,
            String prompt
    ) {
        this.course = course;
        this.module = module;
        this.type = AiGenerationType.MODULE_DRAFT;
        this.requestedBy = requestedBy;
        this.documentSource = documentSource;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.requirements = requirements;
        this.prompt = prompt;
        this.status = AiGenerationStatus.PENDING;
    }

    public AiGenerationLog(
            Course course,
            User requestedBy,
            DocumentSource documentSource,
            Integer pageStart,
            Integer pageEnd,
            String requirements,
            String prompt
    ) {
        this.course = course;
        this.module = null;
        this.type = AiGenerationType.COURSE_OUTLINE;
        this.requestedBy = requestedBy;
        this.documentSource = documentSource;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.requirements = requirements;
        this.prompt = prompt;
        this.status = AiGenerationStatus.PENDING;
    }

    public void markRunning() {
        this.status = AiGenerationStatus.RUNNING;
        this.errorMessage = null;
    }

    public void updatePrompt(String prompt) {
        this.prompt = prompt;
    }

    public void markSuccess(String rawResponse) {
        this.status = AiGenerationStatus.SUCCESS;
        this.rawResponse = rawResponse;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = AiGenerationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.rawResponse = null;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public CourseModule getModule() {
        return module;
    }

    public AiGenerationType getType() {
        return type;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public DocumentSource getDocumentSource() {
        return documentSource;
    }

    public Integer getPageStart() {
        return pageStart;
    }

    public Integer getPageEnd() {
        return pageEnd;
    }

    public String getRequirements() {
        return requirements;
    }

    public String getPrompt() {
        return prompt;
    }

    public AiGenerationStatus getStatus() {
        return status;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
