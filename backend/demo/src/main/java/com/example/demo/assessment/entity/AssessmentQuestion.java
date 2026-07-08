package com.example.demo.assessment.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "assessment_question")
public class AssessmentQuestion extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false, columnDefinition = "text")
    private String questionText;

    @Column(nullable = false)
    private Integer sortOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<AssessmentOption> options = new ArrayList<>();

    protected AssessmentQuestion() {
    }

    public AssessmentQuestion(String questionText, Integer sortOrder) {
        this.questionText = questionText;
        this.sortOrder = sortOrder;
    }

    void assignToAssessment(Assessment assessment) {
        this.assessment = assessment;
    }

    public void addOption(AssessmentOption option) {
        options.add(option);
        option.assignToQuestion(this);
    }

    public Long getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public String getQuestionText() {
        return questionText;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public List<AssessmentOption> getOptions() {
        return Collections.unmodifiableList(options);
    }
}
