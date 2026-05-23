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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "course_review")
public class CourseReview extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseReviewDecision decision;

    @Column(columnDefinition = "text")
    private String feedback;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CourseReviewComment> comments = new ArrayList<>();

    protected CourseReview() {
    }

    public CourseReview(Course course, User reviewer, CourseReviewDecision decision, String feedback) {
        this.course = course;
        this.reviewer = reviewer;
        this.decision = decision;
        this.feedback = feedback;
    }

    public void addComment(CourseReviewComment comment) {
        comments.add(comment);
        comment.assignToReview(this);
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public User getReviewer() {
        return reviewer;
    }

    public CourseReviewDecision getDecision() {
        return decision;
    }

    public String getFeedback() {
        return feedback;
    }

    public List<CourseReviewComment> getComments() {
        return Collections.unmodifiableList(comments);
    }
}
