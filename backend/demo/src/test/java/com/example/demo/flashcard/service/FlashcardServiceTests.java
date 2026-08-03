package com.example.demo.flashcard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.example.demo.gamification.dto.GamificationRewardDto;
import com.example.demo.gamification.service.GamificationService;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTests {

    @Mock
    private FlashcardDeckRepository deckRepository;

    @Mock
    private FlashcardCardRepository cardRepository;

    @Mock
    private LearnerFlashcardSrsCardRepository srsCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private FlashcardService service;

    private FlashcardCard mockCard(Long id, String front, String tagsCsv, FlashcardDeck deck) {
        FlashcardCard card = mock(FlashcardCard.class);
        lenient().when(card.getId()).thenReturn(id);
        lenient().when(card.getFront()).thenReturn(front);
        lenient().when(card.getBackText()).thenReturn("back-" + id);
        lenient().when(card.getTagsCsv()).thenReturn(tagsCsv);
        lenient().when(card.getDeck()).thenReturn(deck);
        return card;
    }

    // UTC-17_TC-01
    @Test
    void listDecksReturnsSummariesWithCardCounts() {
        FlashcardDeck deck = mock(FlashcardDeck.class);
        when(deck.getId()).thenReturn(1L);
        when(deck.getTitle()).thenReturn("Trigonometry Basics");
        when(deck.getTag()).thenReturn("TRIG");
        when(deck.getSortOrder()).thenReturn(1);

        when(deckRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(deck));
        when(cardRepository.countByDeckId(1L)).thenReturn(12L);

        List<FlashcardDeckSummaryDto> decks = service.listDecks();

        assertThat(decks).hasSize(1);
        assertThat(decks.get(0).cardCount()).isEqualTo(12L);
        assertThat(decks.get(0).tag()).isEqualTo("TRIG");
    }

    // UTC-17_TC-02
    @Test
    void getDeckDetailSplitsTagsAndHandlesBlankCsv() {
        FlashcardDeck deck = mock(FlashcardDeck.class);
        when(deck.getId()).thenReturn(2L);
        when(deck.getTitle()).thenReturn("Derivatives");
        when(deck.getTag()).thenReturn("CALC");

        FlashcardCard taggedCard = mockCard(10L, "d/dx(x^2)", "power-rule, derivative", deck);
        FlashcardCard untaggedCard = mockCard(11L, "d/dx(sin x)", "  ", deck);

        when(deckRepository.findById(2L)).thenReturn(Optional.of(deck));
        when(cardRepository.findByDeckIdOrderBySortOrderAscIdAsc(2L)).thenReturn(List.of(taggedCard, untaggedCard));

        FlashcardDeckDetailDto detail = service.getDeckDetail(2L);

        assertThat(detail.cards()).hasSize(2);
        assertThat(detail.cards().get(0).tags()).containsExactly("power-rule", "derivative");
        assertThat(detail.cards().get(1).tags()).isEmpty();
    }

    // UTC-17_TC-03
    @Test
    void getDeckDetailUnknownDeckThrowsResourceNotFoundException() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDeckDetail(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Flashcard deck not found: 99");
    }

    // UTC-17_TC-04
    @Test
    void syncSrsQueueForUserOnlyEnrollsUntrackedCards() {
        UUID userId = UUID.randomUUID();
        FlashcardCard tracked = mock(FlashcardCard.class);
        when(tracked.getId()).thenReturn(1L);
        FlashcardCard untracked = mock(FlashcardCard.class);
        when(untracked.getId()).thenReturn(2L);

        when(srsCardRepository.findTrackedCardIdsByUserId(userId)).thenReturn(Set.of(1L));
        when(cardRepository.findAll()).thenReturn(List.of(tracked, untracked));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        service.syncSrsQueueForUser(userId);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<LearnerFlashcardSrsCard>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(srsCardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    // UTC-17_TC-05
    @Test
    void getQueuePageReturnsDueItemsAndCounts() {
        UUID userId = UUID.randomUUID();
        when(srsCardRepository.findTrackedCardIdsByUserId(userId)).thenReturn(Set.of());
        when(cardRepository.findAll()).thenReturn(List.of());

        FlashcardDeck deck = mock(FlashcardDeck.class);
        when(deck.getTitle()).thenReturn("Trigonometry Basics");
        when(deck.getTag()).thenReturn("TRIG");
        FlashcardCard card = mockCard(1L, "sin(90)", "", deck);

        LearnerFlashcardSrsCard srsCard = mock(LearnerFlashcardSrsCard.class);
        when(srsCard.getId()).thenReturn(500L);
        when(srsCard.getCard()).thenReturn(card);
        when(srsCard.getDueAt()).thenReturn(Instant.now());

        Pageable pageable = PageRequest.of(0, 5);
        Page<LearnerFlashcardSrsCard> page = new PageImpl<>(List.of(srsCard), pageable, 1);
        when(srsCardRepository.findByUserUserIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
                org.mockito.ArgumentMatchers.eq(userId), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);
        when(srsCardRepository.countByUserUserId(userId)).thenReturn(9L);
        when(srsCardRepository.countByUserUserIdAndDueAtBetween(org.mockito.ArgumentMatchers.eq(userId), any(), any()))
                .thenReturn(3L);

        SrsQueuePageDto result = service.getQueuePage(userId, 0, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.totalTracked()).isEqualTo(9L);
        assertThat(result.totalDueLaterToday()).isEqualTo(3L);
    }

    // UTC-18_TC-01
    @Test
    void submitReviewAgainAdvancesDueDateByOneMinuteWithoutReward() {
        UUID userId = UUID.randomUUID();
        LearnerFlashcardSrsCard srsCard = new LearnerFlashcardSrsCard(mock(User.class), mock(FlashcardCard.class), Instant.now());
        when(srsCardRepository.findByIdAndUserUserId(50L, userId)).thenReturn(Optional.of(srsCard));

        Instant before = Instant.now();
        SrsReviewResultDto result = service.submitReview(userId, 50L, SrsOutcome.AGAIN);
        Instant after = Instant.now();

        assertThat(result.outcome()).isEqualTo("AGAIN");
        assertThat(result.nextDueAt()).isBetween(before.plus(1, ChronoUnit.MINUTES).minusSeconds(2),
                after.plus(1, ChronoUnit.MINUTES).plusSeconds(2));
        assertThat(result.reward()).isEqualTo(GamificationRewardDto.empty());
        verify(gamificationService, never()).notifyFlashcardsMemorized(any(), anyInt());
    }

    // UTC-18_TC-02
    @Test
    void submitReviewEasyAdvancesFourDaysAndNotifiesGamificationService() {
        UUID userId = UUID.randomUUID();
        LearnerFlashcardSrsCard srsCard = new LearnerFlashcardSrsCard(mock(User.class), mock(FlashcardCard.class), Instant.now());
        when(srsCardRepository.findByIdAndUserUserId(51L, userId)).thenReturn(Optional.of(srsCard));
        when(srsCardRepository.countByUserUserIdAndLastOutcome(userId, SrsOutcome.EASY)).thenReturn(4L);
        GamificationRewardDto reward = new GamificationRewardDto(15, false, 2, List.of());
        when(gamificationService.notifyFlashcardsMemorized(userId, 4)).thenReturn(reward);

        Instant before = Instant.now();
        SrsReviewResultDto result = service.submitReview(userId, 51L, SrsOutcome.EASY);

        assertThat(result.outcome()).isEqualTo("EASY");
        assertThat(result.nextDueAt()).isAfter(before.plus(3, ChronoUnit.DAYS));
        assertThat(result.reward()).isEqualTo(reward);
        verify(gamificationService).notifyFlashcardsMemorized(userId, 4);
    }

    // UTC-18_TC-03
    @Test
    void submitReviewUnknownSrsCardThrowsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(srsCardRepository.findByIdAndUserUserId(999L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitReview(userId, 999L, SrsOutcome.GOOD))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SRS card not found: 999");
    }

    // UTC-18_TC-04
    @Test
    void getDueSessionCardsSyncsQueueThenReturnsAllDueCards() {
        UUID userId = UUID.randomUUID();
        when(srsCardRepository.findTrackedCardIdsByUserId(userId)).thenReturn(Set.of());
        when(cardRepository.findAll()).thenReturn(List.of());

        FlashcardDeck deck = mock(FlashcardDeck.class);
        when(deck.getTitle()).thenReturn("Trigonometry Basics");
        when(deck.getTag()).thenReturn("TRIG");
        FlashcardCard card = mockCard(2L, "cos(0)", "", deck);
        LearnerFlashcardSrsCard due = mock(LearnerFlashcardSrsCard.class);
        when(due.getId()).thenReturn(600L);
        when(due.getCard()).thenReturn(card);
        when(due.getDueAt()).thenReturn(Instant.now());

        when(srsCardRepository.findByUserUserIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
                org.mockito.ArgumentMatchers.eq(userId), any(Instant.class)))
                .thenReturn(List.of(due));

        List<SrsCardDto> result = service.getDueSessionCards(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).srsCardId()).isEqualTo(600L);
    }
}
