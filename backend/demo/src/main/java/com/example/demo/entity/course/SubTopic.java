package com.example.demo.entity.course;

import com.example.demo.entity.BaseTimestampEntity;
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
import jakarta.persistence.CascadeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "sub_topic")
public class SubTopic extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubTopicSourceType sourceType;

    private Integer pageStart;

    private Integer pageEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InteractionType interactionType = InteractionType.NONE;

    @Column(columnDefinition = "text")
    private String interactionPrompt;

    @Column(columnDefinition = "text")
    private String interactionConfig;

    @OneToMany(mappedBy = "subTopic", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<SubTopicAsset> assets = new ArrayList<>();

    protected SubTopic() {
    }

    public SubTopic(
            String title,
            String content,
            Integer sortOrder,
            SubTopicSourceType sourceType,
            Integer pageStart,
            Integer pageEnd
    ) {
        this.title = title;
        this.content = content;
        this.sortOrder = sortOrder;
        this.sourceType = sourceType;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
    }

    public SubTopic(
            String title,
            String content,
            Integer sortOrder,
            SubTopicSourceType sourceType,
            Integer pageStart,
            Integer pageEnd,
            InteractionType interactionType,
            String interactionPrompt
    ) {
        this(title, content, sortOrder, sourceType, pageStart, pageEnd, interactionType, interactionPrompt, null);
    }

    public SubTopic(
            String title,
            String content,
            Integer sortOrder,
            SubTopicSourceType sourceType,
            Integer pageStart,
            Integer pageEnd,
            InteractionType interactionType,
            String interactionPrompt,
            String interactionConfig
    ) {
        this(title, content, sortOrder, sourceType, pageStart, pageEnd);
        this.interactionType = interactionType == null ? InteractionType.NONE : interactionType;
        this.interactionPrompt = interactionPrompt;
        this.interactionConfig = interactionConfig;
    }

    void assignToModule(CourseModule module) {
        this.module = module;
    }

    public void updateDetails(String title, String content, Integer sortOrder, Integer pageStart, Integer pageEnd) {
        this.title = title;
        this.content = content;
        this.sortOrder = sortOrder;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.sourceType = SubTopicSourceType.MANUAL;
    }

    public void updateInteraction(InteractionType interactionType, String interactionPrompt) {
        updateInteraction(interactionType, interactionPrompt, interactionConfig);
    }

    public void updateInteraction(InteractionType interactionType, String interactionPrompt, String interactionConfig) {
        this.interactionType = interactionType == null ? InteractionType.NONE : interactionType;
        this.interactionPrompt = interactionPrompt;
        this.interactionConfig = interactionConfig;
    }

    public void addAsset(SubTopicAsset asset) {
        assets.add(asset);
        asset.assignToSubTopic(this);
    }

    public Long getId() {
        return id;
    }

    public CourseModule getModule() {
        return module;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public SubTopicSourceType getSourceType() {
        return sourceType;
    }

    public Integer getPageStart() {
        return pageStart;
    }

    public Integer getPageEnd() {
        return pageEnd;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public String getInteractionPrompt() {
        return interactionPrompt;
    }

    public String getInteractionConfig() {
        return interactionConfig;
    }

    public List<SubTopicAsset> getAssets() {
        return Collections.unmodifiableList(assets);
    }
}
