package com.example.demo.learningassistant.service;

import com.example.demo.learningassistant.dto.AssistantDtos.*;
import com.example.demo.learningassistant.entity.AssistantMode;
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
    private static final int MAX_RESPONSE_ATTEMPTS = 2;
    private static final int SSE_DELTA_SIZE = 64;
    private final AssistantPersistenceService persistence;
    private final AssistantPromptBuilder promptBuilder;
    private final AssistantResponseFormatter responseFormatter;
    private final AssistantAiClient aiClient;
    private final Executor executor;

    public LearningAssistantService(AssistantPersistenceService persistence, AssistantPromptBuilder promptBuilder,
            AssistantResponseFormatter responseFormatter, AssistantAiClient aiClient,
            @Qualifier("assistantTaskExecutor") Executor executor) {
        this.persistence = persistence;
        this.promptBuilder = promptBuilder;
        this.responseFormatter = responseFormatter;
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

    void stream(PreparedMessage prepared, SseEmitter emitter) {
        AssistantStreamContext context = prepared.context();
        try {
            sendEvent(emitter, "message-start", Map.of("conversationId", context.conversationId(),
                    "userMessageId", context.userMessageId(), "assistantMessageId", context.assistantMessageId()));
            String prompt = promptBuilder.build(context, prepared.history());
            String answer = generateValidAnswer(context.mode(), prompt);
            emitAnswer(emitter, answer);
            persistence.complete(context.assistantMessageId(), answer);
            sendEvent(emitter, "message-complete", Map.of("assistantMessageId", context.assistantMessageId(),
                    "content", answer, "mode", context.mode().name()));
            emitter.complete();
        } catch (StreamClosedException exception) {
            // A failed SSE write is terminal. Spring handles connection cleanup.
            persistence.fail(context.assistantMessageId());
        } catch (Exception exception) {
            persistence.fail(context.assistantMessageId());
            try {
                sendEvent(emitter, "error", Map.of("code", "AI_STREAM_FAILED",
                        "message", "AI assistant is temporarily unavailable. Please try again."));
                emitter.complete();
            } catch (StreamClosedException ignored) {
                // The client disconnected while we were reporting the failure.
            } catch (RuntimeException ignored) {
                emitter.completeWithError(exception);
            }
        }
    }

    private String generateValidAnswer(AssistantMode mode, String prompt) {
        AssistantResponseFormatter.InvalidAssistantResponseException lastFailure = null;
        for (int attempt = 0; attempt < MAX_RESPONSE_ATTEMPTS; attempt++) {
            StringBuilder structuredResponse = new StringBuilder();
            aiClient.stream(prompt, structuredResponse::append);
            try {
                return responseFormatter.format(mode, structuredResponse.toString());
            } catch (AssistantResponseFormatter.InvalidAssistantResponseException exception) {
                lastFailure = exception;
            }
        }
        throw lastFailure == null ? new IllegalStateException("AI returned an invalid response") : lastFailure;
    }

    private void emitAnswer(SseEmitter emitter, String answer) {
        for (int start = 0; start < answer.length(); start += SSE_DELTA_SIZE) {
            int end = Math.min(start + SSE_DELTA_SIZE, answer.length());
            sendEvent(emitter, "content-delta", Map.of("text", answer.substring(start, end)));
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
