package com.example.demo.flashcard.dto;

import java.util.List;

public record FlashcardCardDto(
        Long id,
        String front,
        String backText,
        String note,
        String example,
        List<String> tags
) {
}
