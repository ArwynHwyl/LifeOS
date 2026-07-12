package com.example.demo.flashcard.entity;

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
@Table(name = "flashcard_card")
public class FlashcardCard extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private FlashcardDeck deck;

    @Column(nullable = false, length = 255)
    private String front;

    @Column(nullable = false, columnDefinition = "text")
    private String backText;

    @Column(columnDefinition = "text")
    private String note;

    @Column(columnDefinition = "text")
    private String example;

    @Column(length = 255)
    private String tagsCsv;

    @Column(nullable = false)
    private Integer sortOrder;

    protected FlashcardCard() {
    }

    public FlashcardCard(String front, String backText, String note, String example, String tagsCsv, Integer sortOrder) {
        this.front = front;
        this.backText = backText;
        this.note = note;
        this.example = example;
        this.tagsCsv = tagsCsv;
        this.sortOrder = sortOrder;
    }

    void assignToDeck(FlashcardDeck deck) {
        this.deck = deck;
    }

    public Long getId() {
        return id;
    }

    public FlashcardDeck getDeck() {
        return deck;
    }

    public String getFront() {
        return front;
    }

    public String getBackText() {
        return backText;
    }

    public String getNote() {
        return note;
    }

    public String getExample() {
        return example;
    }

    public String getTagsCsv() {
        return tagsCsv;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
