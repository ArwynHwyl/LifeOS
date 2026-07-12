package com.example.demo.flashcard.dto;

import java.util.List;

public record FlashcardDeckDetailDto(
        Long id,
        String title,
        String description,
        String tag,
        List<FlashcardCardDto> cards
) {
}
