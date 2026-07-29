package com.example.demo.learningassistant.entity;

import com.example.demo.shared.entity.BaseTimestampEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "assistant_message", indexes =
        @Index(name = "idx_assistant_message_conversation_created", columnList = "conversation_id,created_at"))
public class AssistantMessage extends BaseTimestampEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AssistantConversation conversation;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AssistantMessageRole role;

    @Enumerated(EnumType.STRING) @Column(length = 20)
    private AssistantMode mode;

    @Column(columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String selectedText;

    @Column(length = 50)
    private String suggestionKey;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private AssistantMessageStatus status;

    @Enumerated(EnumType.STRING) @Column(length = 30)
    private AssistantFeedback feedback;

    protected AssistantMessage() {}

    public AssistantMessage(AssistantConversation conversation, AssistantMessageRole role, AssistantMode mode,
                            String content, String selectedText, String suggestionKey, AssistantMessageStatus status) {
        this.conversation = conversation;
        this.role = role;
        this.mode = mode;
        this.content = content;
        this.selectedText = selectedText;
        this.suggestionKey = suggestionKey;
        this.status = status;
    }

    public void complete(String content) { this.content = content; this.status = AssistantMessageStatus.COMPLETED; }
    public void fail() { this.status = AssistantMessageStatus.FAILED; }
    public void updateFeedback(AssistantFeedback feedback) { this.feedback = feedback; }
    public Long getId() { return id; }
    public AssistantConversation getConversation() { return conversation; }
    public AssistantMessageRole getRole() { return role; }
    public AssistantMode getMode() { return mode; }
    public String getContent() { return content; }
    public String getSelectedText() { return selectedText; }
    public String getSuggestionKey() { return suggestionKey; }
    public AssistantMessageStatus getStatus() { return status; }
    public AssistantFeedback getFeedback() { return feedback; }
}
