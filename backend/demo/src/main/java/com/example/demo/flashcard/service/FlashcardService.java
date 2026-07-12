package com.example.demo.flashcard.service;

import com.example.demo.flashcard.dto.FlashcardCardDto;
import com.example.demo.flashcard.dto.FlashcardDeckDetailDto;
import com.example.demo.flashcard.dto.FlashcardDeckSummaryDto;
import com.example.demo.flashcard.dto.SrsCardDto;
import com.example.demo.flashcard.dto.SrsQueuePageDto;
import com.example.demo.flashcard.dto.SrsReviewResultDto;
import com.example.demo.flashcard.entity.FlashcardCard;
import com.example.demo.flashcard.entity.FlashcardDeck;
import com.example.demo.flashcard.entity.LearnerFlashcardSrsCard;
import com.example.demo.flashcard.entity.SrsOutcome;
import com.example.demo.flashcard.repository.FlashcardCardRepository;
import com.example.demo.flashcard.repository.FlashcardDeckRepository;
import com.example.demo.flashcard.repository.LearnerFlashcardSrsCardRepository;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlashcardService {

    private static final Duration AGAIN_INTERVAL = Duration.ofMinutes(1);
    private static final Duration HARD_INTERVAL = Duration.ofMinutes(10);
    private static final Duration GOOD_INTERVAL = Duration.ofDays(1);
    private static final Duration EASY_INTERVAL = Duration.ofDays(4);

    private final FlashcardDeckRepository deckRepository;
    private final FlashcardCardRepository cardRepository;
    private final LearnerFlashcardSrsCardRepository srsCardRepository;
    private final UserRepository userRepository;

    public FlashcardService(
            FlashcardDeckRepository deckRepository,
            FlashcardCardRepository cardRepository,
            LearnerFlashcardSrsCardRepository srsCardRepository,
            UserRepository userRepository
    ) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.srsCardRepository = srsCardRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<FlashcardDeckSummaryDto> listDecks() {
        return deckRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(deck -> new FlashcardDeckSummaryDto(
                        deck.getId(),
                        deck.getTitle(),
                        deck.getDescription(),
                        deck.getTag(),
                        cardRepository.countByDeckId(deck.getId()),
                        deck.getSortOrder()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public FlashcardDeckDetailDto getDeckDetail(Long deckId) {
        FlashcardDeck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard deck not found: " + deckId));
        List<FlashcardCardDto> cards = cardRepository.findByDeckIdOrderBySortOrderAscIdAsc(deckId).stream()
                .map(this::toCardDto)
                .toList();
        return new FlashcardDeckDetailDto(deck.getId(), deck.getTitle(), deck.getDescription(), deck.getTag(), cards);
    }

    @Transactional
    public void syncSrsQueueForUser(UUID userId) {
        Set<Long> trackedCardIds = srsCardRepository.findTrackedCardIdsByUserId(userId);
        List<FlashcardCard> untracked = cardRepository.findAll().stream()
                .filter(card -> !trackedCardIds.contains(card.getId()))
                .toList();
        if (untracked.isEmpty()) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Instant now = Instant.now();
        List<LearnerFlashcardSrsCard> newRows = untracked.stream()
                .map(card -> new LearnerFlashcardSrsCard(user, card, now))
                .toList();
        srsCardRepository.saveAll(newRows);
    }

    @Transactional
    public SrsQueuePageDto getQueuePage(UUID userId, int page, int size) {
        syncSrsQueueForUser(userId);
        Instant now = Instant.now();
        Instant endOfToday = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Pageable pageable = PageRequest.of(page, size);
        Page<LearnerFlashcardSrsCard> duePage = srsCardRepository
                .findByUserUserIdAndDueAtLessThanEqualOrderByDueAtAsc(userId, now, pageable);

        long totalTracked = srsCardRepository.countByUserUserId(userId);
        long totalDueLaterToday = srsCardRepository.countByUserUserIdAndDueAtBetween(userId, now, endOfToday);

        List<SrsCardDto> items = duePage.getContent().stream().map(this::toSrsCardDto).toList();
        return new SrsQueuePageDto(items, page, size, duePage.getTotalElements(), totalTracked, totalDueLaterToday);
    }

    @Transactional
    public List<SrsCardDto> getDueSessionCards(UUID userId) {
        syncSrsQueueForUser(userId);
        Instant now = Instant.now();
        return srsCardRepository.findByUserUserIdAndDueAtLessThanEqualOrderByDueAtAsc(userId, now).stream()
                .map(this::toSrsCardDto)
                .toList();
    }

    @Transactional
    public SrsReviewResultDto submitReview(UUID userId, Long srsCardId, SrsOutcome outcome) {
        LearnerFlashcardSrsCard srsCard = srsCardRepository.findByIdAndUserUserId(srsCardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SRS card not found: " + srsCardId));
        Instant now = Instant.now();
        Instant nextDueAt = now.plus(intervalFor(outcome));
        srsCard.applyReview(outcome, now, nextDueAt);
        srsCardRepository.save(srsCard);
        return new SrsReviewResultDto(srsCard.getId(), outcome.name(), nextDueAt);
    }

    private Duration intervalFor(SrsOutcome outcome) {
        return switch (outcome) {
            case AGAIN -> AGAIN_INTERVAL;
            case HARD -> HARD_INTERVAL;
            case GOOD -> GOOD_INTERVAL;
            case EASY -> EASY_INTERVAL;
        };
    }

    private FlashcardCardDto toCardDto(FlashcardCard card) {
        return new FlashcardCardDto(
                card.getId(), card.getFront(), card.getBackText(), card.getNote(), card.getExample(),
                splitTags(card.getTagsCsv())
        );
    }

    private SrsCardDto toSrsCardDto(LearnerFlashcardSrsCard srsCard) {
        FlashcardCard card = srsCard.getCard();
        return new SrsCardDto(
                srsCard.getId(),
                card.getId(),
                card.getFront(),
                card.getBackText(),
                card.getNote(),
                card.getExample(),
                splitTags(card.getTagsCsv()),
                card.getDeck().getTitle(),
                card.getDeck().getTag(),
                srsCard.getDueAt()
        );
    }

    private List<String> splitTags(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }
}
