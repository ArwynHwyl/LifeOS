package com.example.demo.gamification.repository;

import com.example.demo.gamification.entity.UserAchievement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserUserId(UUID userId);

    boolean existsByUserUserIdAndAchievementId(UUID userId, Long achievementId);
}
