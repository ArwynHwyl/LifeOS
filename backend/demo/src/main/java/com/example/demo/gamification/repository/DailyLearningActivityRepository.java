package com.example.demo.gamification.repository;

import com.example.demo.gamification.entity.DailyLearningActivity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyLearningActivityRepository extends JpaRepository<DailyLearningActivity, Long> {

    Optional<DailyLearningActivity> findByUserUserIdAndActivityDate(UUID userId, LocalDate activityDate);

    List<DailyLearningActivity> findByUserUserIdAndActivityDateBetweenOrderByActivityDateAsc(
            UUID userId, LocalDate startDate, LocalDate endDate);
}
