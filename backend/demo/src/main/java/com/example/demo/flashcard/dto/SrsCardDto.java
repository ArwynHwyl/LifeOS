package com.example.demo.flashcard.dto;

import java.time.Instant;
import java.util.List;

public record SrsCardDto(
        Long srsCardId,
        Long cardId,
        String front,
        String backText,
        String note,
        String example,
        List<String> tags,
        String deckTitle,
        String deckTag,
        Instant dueAt
) {
}
