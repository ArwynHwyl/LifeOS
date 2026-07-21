package com.example.demo.flashcard.repository;

import com.example.demo.flashcard.entity.LearnerFlashcardSrsCard;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearnerFlashcardSrsCardRepository extends JpaRepository<LearnerFlashcardSrsCard, Long> {

    @Query("select s.card.id from LearnerFlashcardSrsCard s where s.user.userId = :userId")
    Set<Long> findTrackedCardIdsByUserId(@Param("userId") UUID userId);

    Optional<LearnerFlashcardSrsCard> findByIdAndUserUserId(Long id, UUID userId);

    long countByUserUserId(UUID userId);

    long countByUserUserIdAndDueAtBetween(UUID userId, Instant start, Instant end);

    List<LearnerFlashcardSrsCard> findByUserUserIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(UUID userId, Instant now);

    Page<LearnerFlashcardSrsCard> findByUserUserIdAndDueAtLessThanEqualOrderByDueAtAscIdAsc(
            UUID userId, Instant now, Pageable pageable);
}
