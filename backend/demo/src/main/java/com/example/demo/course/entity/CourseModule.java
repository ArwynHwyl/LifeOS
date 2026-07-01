package com.example.demo.course.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "course_module")
public class CourseModule extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContentDepth contentDepth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InteractionType interactionType = InteractionType.NONE;

    @Column(columnDefinition = "text")
    private String interactionPrompt;

    @Column(columnDefinition = "text")
    private String interactionConfig;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<SubTopic> subTopics = new ArrayList<>();

    protected CourseModule() {
    }

    public CourseModule(String title, String description, Integer sortOrder, ContentDepth contentDepth) {
        this.title = title;
        this.description = description;
        this.sortOrder = sortOrder;
        this.contentDepth = contentDepth;
    }

    public CourseModule(
            String title,
            String description,
            Integer sortOrder,
            ContentDepth contentDepth,
            InteractionType interactionType,
            String interactionPrompt
    ) {
        this(title, description, sortOrder, contentDepth, interactionType, interactionPrompt, null);
    }

    public CourseModule(
            String title,
            String description,
            Integer sortOrder,
            ContentDepth contentDepth,
            InteractionType interactionType,
            String interactionPrompt,
            String interactionConfig
    ) {
        this(title, description, sortOrder, contentDepth);
        this.interactionType = interactionType == null ? InteractionType.NONE : interactionType;
        this.interactionPrompt = interactionPrompt;
        this.interactionConfig = interactionConfig;
    }

    void assignToCourse(Course course) {
        this.course = course;
    }

    public void updateDetails(String title, String description, Integer sortOrder, ContentDepth contentDepth) {
        this.title = title;
        this.description = description;
        this.sortOrder = sortOrder;
        this.contentDepth = contentDepth;
    }

    public void updateInteraction(InteractionType interactionType, String interactionPrompt) {
        updateInteraction(interactionType, interactionPrompt, interactionConfig);
    }

    public void updateInteraction(InteractionType interactionType, String interactionPrompt, String interactionConfig) {
        this.interactionType = interactionType == null ? InteractionType.NONE : interactionType;
        this.interactionPrompt = interactionPrompt;
        this.interactionConfig = interactionConfig;
    }

    public void addSubTopic(SubTopic subTopic) {
        subTopics.add(subTopic);
        subTopic.assignToModule(this);
    }

    public void removeSubTopic(SubTopic subTopic) {
        subTopics.remove(subTopic);
        subTopic.assignToModule(null);
    }

    public void clearSubTopics() {
        for (SubTopic subTopic : subTopics) {
            subTopic.assignToModule(null);
        }
        subTopics.clear();
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public ContentDepth getContentDepth() {
        return contentDepth;
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

    public List<SubTopic> getSubTopics() {
        return Collections.unmodifiableList(subTopics);
    }
}
