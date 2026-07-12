package com.example.demo.flashcard.repository;

import com.example.demo.flashcard.entity.FlashcardDeck;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeck, Long> {

    List<FlashcardDeck> findAllByOrderBySortOrderAscIdAsc();
}
