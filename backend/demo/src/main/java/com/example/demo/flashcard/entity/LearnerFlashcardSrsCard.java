package com.example.demo.flashcard.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "learner_flashcard_srs_card",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_learner_flashcard_srs_card_user_card",
                columnNames = {"user_id", "card_id"}
        )
)
public class LearnerFlashcardSrsCard extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private FlashcardCard card;

    @Column(nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SrsOutcome lastOutcome;

    private Instant lastReviewedAt;

    @Column(nullable = false)
    private int reviewCount;

    protected LearnerFlashcardSrsCard() {
    }

    public LearnerFlashcardSrsCard(User user, FlashcardCard card, Instant dueAt) {
        this.user = user;
        this.card = card;
        this.dueAt = dueAt;
        this.reviewCount = 0;
    }

    public void applyReview(SrsOutcome outcome, Instant reviewedAt, Instant nextDueAt) {
        this.lastOutcome = outcome;
        this.lastReviewedAt = reviewedAt;
        this.dueAt = nextDueAt;
        this.reviewCount += 1;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public FlashcardCard getCard() {
        return card;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public SrsOutcome getLastOutcome() {
        return lastOutcome;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public int getReviewCount() {
        return reviewCount;
    }
}
