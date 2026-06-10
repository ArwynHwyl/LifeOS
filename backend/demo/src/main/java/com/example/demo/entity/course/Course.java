package com.example.demo.entity.course;

import com.example.demo.entity.BaseTimestampEntity;
import com.example.demo.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "course")
public class Course extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 80)
    private String coverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    private Instant publishedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<CourseModule> modules = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC, id DESC")
    private List<DocumentSource> documentSources = new ArrayList<>();

    protected Course() {
    }

    public Course(String title, String description, User createdBy) {
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.status = CourseStatus.DRAFT;
    }

    public void updateDetails(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void updateCover(String coverId) {
        this.coverId = coverId;
    }

    public String getCoverId() {
        return coverId;
    }

    public void submitForReview() {
        status = CourseStatus.PENDING_REVIEW;
        approvedBy = null;
        publishedAt = null;
    }

    public void markNeedsRevision() {
        status = CourseStatus.NEED_REVISION;
        approvedBy = null;
        publishedAt = null;
    }

    public void reopenDraftForAdminEdit() {
        status = CourseStatus.DRAFT;
        approvedBy = null;
        publishedAt = null;
    }

    public void approve(User reviewer) {
        status = CourseStatus.PUBLISHED;
        approvedBy = reviewer;
        publishedAt = Instant.now();
    }

    public void publish() {
        status = CourseStatus.PUBLISHED;
        publishedAt = Instant.now();
    }

    public void addModule(CourseModule module) {
        modules.add(module);
        module.assignToCourse(this);
    }

    public void removeModule(CourseModule module) {
        modules.remove(module);
        module.assignToCourse(null);
    }

    public void clearModules() {
        for (CourseModule module : modules) {
            module.assignToCourse(null);
        }
        modules.clear();
    }

    public void addDocumentSource(DocumentSource documentSource) {
        documentSources.add(documentSource);
        documentSource.assignToCourse(this);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public CourseStatus getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public List<CourseModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<DocumentSource> getDocumentSources() {
        return Collections.unmodifiableList(documentSources);
    }
}
