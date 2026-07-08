package com.example.demo.flashcard.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "flashcard_deck")
public class FlashcardDeck extends BaseTimestampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 40)
    private String tag;

    @Column(nullable = false)
    private Integer sortOrder;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<FlashcardCard> cards = new ArrayList<>();

    protected FlashcardDeck() {
    }

    public FlashcardDeck(String title, String description, String tag, Integer sortOrder) {
        this.title = title;
        this.description = description;
        this.tag = tag;
        this.sortOrder = sortOrder;
    }

    public void addCard(FlashcardCard card) {
        cards.add(card);
        card.assignToDeck(this);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTag() {
        return tag;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public List<FlashcardCard> getCards() {
        return Collections.unmodifiableList(cards);
    }
}
