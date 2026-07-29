package com.example.demo.learningassistant.service;

import com.example.demo.learningassistant.dto.AssistantDtos.*;
import com.example.demo.learningassistant.service.AssistantPersistenceService.PreparedMessage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class LearningAssistantService {
    private static final long STREAM_TIMEOUT_MS = 90_000L;
    private final AssistantPersistenceService persistence;
    private final AssistantPromptBuilder promptBuilder;
    private final AssistantAiClient aiClient;
    private final Executor executor;

    public LearningAssistantService(AssistantPersistenceService persistence, AssistantPromptBuilder promptBuilder,
            AssistantAiClient aiClient, @Qualifier("assistantTaskExecutor") Executor executor) {
        this.persistence = persistence;
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
        this.executor = executor;
    }

    public ConversationDto open(UUID userId, Long courseId, Long subTopicId) {
        return persistence.open(userId, courseId, subTopicId);
    }

    public SseEmitter send(UUID userId, Long courseId, Long subTopicId, SendMessageRequest request) {
        PreparedMessage prepared = persistence.prepare(userId, courseId, subTopicId, request);
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        executor.execute(() -> stream(prepared, emitter));
        return emitter;
    }

    public FeedbackResponse feedback(UUID userId, Long messageId, FeedbackRequest request) {
        return persistence.feedback(userId, messageId, request);
    }

    public List<WeaknessDto> weaknesses(UUID userId) { return persistence.weaknesses(userId); }

    private void stream(PreparedMessage prepared, SseEmitter emitter) {
        AssistantStreamContext context = prepared.context();
        StringBuilder answer = new StringBuilder();
        try {
            sendEvent(emitter, "message-start", Map.of("conversationId", context.conversationId(),
                    "userMessageId", context.userMessageId(), "assistantMessageId", context.assistantMessageId()));
            String prompt = promptBuilder.build(context, prepared.history());
            aiClient.stream(prompt, delta -> {
                answer.append(delta);
                sendEvent(emitter, "content-delta", Map.of("text", delta));
            });
            if (answer.toString().isBlank()) throw new IllegalStateException("AI returned an empty answer");
            persistence.complete(context.assistantMessageId(), answer.toString().trim());
            sendEvent(emitter, "message-complete", Map.of("assistantMessageId", context.assistantMessageId(),
                    "content", answer.toString().trim(), "mode", context.mode().name()));
            emitter.complete();
        } catch (Exception exception) {
            persistence.fail(context.assistantMessageId());
            try {
                sendEvent(emitter, "error", Map.of("code", "AI_STREAM_FAILED",
                        "message", "AI assistant is temporarily unavailable. Please try again."));
                emitter.complete();
            } catch (RuntimeException ignored) {
                emitter.completeWithError(exception);
            }
        }
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException | IllegalStateException exception) {
            throw new StreamClosedException(exception);
        }
    }

    private static final class StreamClosedException extends RuntimeException {
        private StreamClosedException(Throwable cause) { super(cause); }
    }
}
