package com.example.demo.entity.course;

import com.example.demo.entity.BaseTimestampEntity;
import java.time.Instant;
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
@Table(name = "course_review_comment")
public class CourseReviewComment extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private CourseReview review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private CourseModule module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_topic_id")
    private SubTopic subTopic;

    @Column(nullable = false, columnDefinition = "text")
    private String feedback;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected CourseReviewComment() {
    }

    public CourseReviewComment(CourseModule module, SubTopic subTopic, String feedback) {
        this.module = module;
        this.subTopic = subTopic;
        this.feedback = feedback;
    }

    void assignToReview(CourseReview review) {
        this.review = review;
    }

    public Long getId() {
        return id;
    }

    public CourseReview getReview() {
        return review;
    }

    public CourseModule getModule() {
        return module;
    }

    public SubTopic getSubTopic() {
        return subTopic;
    }

    public String getFeedback() {
        return feedback;
    }

    public boolean isResolved() {
        return resolved;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void markResolved() {
        this.resolved = true;
        this.resolvedAt = Instant.now();
    }

    public void markUnresolved() {
        this.resolved = false;
        this.resolvedAt = null;
    }
}
