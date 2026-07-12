package com.example.demo.gamification.repository;

import com.example.demo.gamification.entity.StreakBonusTier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreakBonusTierRepository extends JpaRepository<StreakBonusTier, Long> {

    List<StreakBonusTier> findAllByOrderByMinStreakDaysAsc();
}
