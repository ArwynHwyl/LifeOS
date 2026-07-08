package com.example.demo.flashcard.dto;

public record FlashcardDeckSummaryDto(
        Long id,
        String title,
        String description,
        String tag,
        long cardCount,
        int sortOrder
) {
}
