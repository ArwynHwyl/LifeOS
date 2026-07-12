package com.example.demo.gamification.repository;

import com.example.demo.gamification.entity.LearnerProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, UUID> {
}
