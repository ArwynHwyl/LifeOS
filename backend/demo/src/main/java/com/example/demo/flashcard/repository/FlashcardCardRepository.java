package com.example.demo.flashcard.repository;

import com.example.demo.flashcard.entity.FlashcardCard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardCardRepository extends JpaRepository<FlashcardCard, Long> {

    List<FlashcardCard> findByDeckIdOrderBySortOrderAscIdAsc(Long deckId);

    long countByDeckId(Long deckId);
}
