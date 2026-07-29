package com.example.demo.learningassistant.repository;

import com.example.demo.learningassistant.entity.AssistantConversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, Long> {
    Optional<AssistantConversation> findByUserUserIdAndSubTopicId(UUID userId, Long subTopicId);
    List<AssistantConversation> findByUserUserId(UUID userId);
}
