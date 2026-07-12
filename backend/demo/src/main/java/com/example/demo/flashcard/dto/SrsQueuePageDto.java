package com.example.demo.flashcard.dto;

import java.util.List;

public record SrsQueuePageDto(
        List<SrsCardDto> items,
        int page,
        int size,
        long totalDueNow,
        long totalTracked,
        long totalDueLaterToday
) {
}
