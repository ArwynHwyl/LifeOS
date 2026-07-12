package com.example.demo.flashcard.controller;

import com.example.demo.flashcard.dto.FlashcardDeckDetailDto;
import com.example.demo.flashcard.dto.FlashcardDeckSummaryDto;
import com.example.demo.flashcard.dto.SrsCardDto;
import com.example.demo.flashcard.dto.SrsQueuePageDto;
import com.example.demo.flashcard.dto.SrsReviewRequest;
import com.example.demo.flashcard.dto.SrsReviewResultDto;
import com.example.demo.flashcard.service.FlashcardService;
import com.example.demo.shared.security.CurrentUser;
import com.example.demo.user.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/learner/flashcards")
@PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'ADMIN')")
public class LearnerFlashcardController {

    private final FlashcardService flashcardService;

    public LearnerFlashcardController(FlashcardService flashcardService) {
        this.flashcardService = flashcardService;
    }

    @GetMapping("/decks")
    public List<FlashcardDeckSummaryDto> listDecks() {
        return flashcardService.listDecks();
    }

    @GetMapping("/decks/{deckId}")
    public FlashcardDeckDetailDto getDeck(@PathVariable Long deckId) {
        return flashcardService.getDeckDetail(deckId);
    }

    @GetMapping("/srs/queue")
    public SrsQueuePageDto getQueue(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return flashcardService.getQueuePage(CurrentUser.id(user), page, size);
    }

    @GetMapping("/srs/due")
    public List<SrsCardDto> getDueSession(@AuthenticationPrincipal User user) {
        return flashcardService.getDueSessionCards(CurrentUser.id(user));
    }

    @PostMapping("/srs/{srsCardId}/review")
    public SrsReviewResultDto submitReview(
            @AuthenticationPrincipal User user,
            @PathVariable Long srsCardId,
            @Valid @RequestBody SrsReviewRequest request
    ) {
        return flashcardService.submitReview(CurrentUser.id(user), srsCardId, request.outcome());
    }
}
