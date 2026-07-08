package com.example.demo.assessment.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
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
@Table(name = "assessment_option")
public class AssessmentOption extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @Column(nullable = false, columnDefinition = "text")
    private String optionText;

    @Column(nullable = false)
    private boolean isCorrect;

    @Column(nullable = false)
    private Integer sortOrder;

    protected AssessmentOption() {
    }

    public AssessmentOption(String optionText, boolean isCorrect, Integer sortOrder) {
        this.optionText = optionText;
        this.isCorrect = isCorrect;
        this.sortOrder = sortOrder;
    }

    void assignToQuestion(AssessmentQuestion question) {
        this.question = question;
    }

    public Long getId() {
        return id;
    }

    public AssessmentQuestion getQuestion() {
        return question;
    }

    public String getOptionText() {
        return optionText;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
