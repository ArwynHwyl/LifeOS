package com.example.demo.course.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
import com.example.demo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_source")
public class DocumentSource extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 255)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String fileType;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(nullable = false, length = 1000)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    private Integer pageCount;

    protected DocumentSource() {
    }

    public DocumentSource(
            String fileName,
            String displayName,
            String fileType,
            Long fileSizeBytes,
            String storagePath,
            User uploadedBy,
            Integer pageCount
    ) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.storagePath = storagePath;
        this.uploadedBy = uploadedBy;
        this.pageCount = pageCount;
    }

    void assignToCourse(Course course) {
        this.course = course;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFileType() {
        return fileType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public Integer getPageCount() {
        return pageCount;
    }
}
