package com.example.demo.learningassistant.repository;

import com.example.demo.learningassistant.entity.AssistantMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, Long> {
    Optional<AssistantMessage> findByIdAndConversationUserUserId(Long id, UUID userId);
    List<AssistantMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);
    List<AssistantMessage> findTop12ByConversationIdAndStatusOrderByCreatedAtDescIdDesc(
            Long conversationId, com.example.demo.learningassistant.entity.AssistantMessageStatus status);
}
