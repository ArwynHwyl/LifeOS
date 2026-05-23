package com.example.demo.entity.course;

import com.example.demo.entity.BaseTimestampEntity;
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
@Table(name = "sub_topic_asset")
public class SubTopicAsset extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_topic_id", nullable = false)
    private SubTopic subTopic;

    @Column(nullable = false, length = 1_000)
    private String storagePath;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String fileType;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(length = 500)
    private String altText;

    protected SubTopicAsset() {
    }

    public SubTopicAsset(String storagePath, String fileName, String fileType, Long fileSizeBytes, String altText) {
        this.storagePath = storagePath;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.altText = altText;
    }

    void assignToSubTopic(SubTopic subTopic) {
        this.subTopic = subTopic;
    }

    public Long getId() {
        return id;
    }

    public SubTopic getSubTopic() {
        return subTopic;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getAltText() {
        return altText;
    }
}
