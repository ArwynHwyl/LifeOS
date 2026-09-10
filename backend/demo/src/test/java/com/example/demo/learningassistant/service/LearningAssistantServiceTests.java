package com.example.demo.learningassistant.service;

import static org.mockito.Mockito.*;

import com.example.demo.learningassistant.dto.AssistantDtos.SendMessageRequest;
import com.example.demo.learningassistant.entity.AssistantMode;
import com.example.demo.learningassistant.service.AssistantPersistenceService.PreparedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class LearningAssistantServiceTests {
    private AssistantPersistenceService persistence;
    private AssistantAiClient aiClient;
    private LearningAssistantService service;
    private AssistantStreamContext context;

    @BeforeEach
    void setUp() {
        persistence = mock(AssistantPersistenceService.class);
        aiClient = mock(AssistantAiClient.class);
        AssistantPromptBuilder promptBuilder = new AssistantPromptBuilder();
        AssistantResponseFormatter responseFormatter = new AssistantResponseFormatter(new ObjectMapper());
        Executor directExecutor = Runnable::run;
        service = new LearningAssistantService(persistence, promptBuilder, responseFormatter, aiClient, directExecutor);
        context = new AssistantStreamContext(1L, 2L, 3L, AssistantMode.EXPLAIN, "Explain", null,
                "Course", "Module", "Topic", "Lesson", null, null, null);
    }

    @Test
    void completedStreamPersistsCombinedAnswer() {
        when(persistence.prepare(any(), eq(10L), eq(20L), any())).thenReturn(new PreparedMessage(context, List.of()));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("{\"teachingPoints\":[\"First second\"],\"examples\":[],");
            consumer.accept("\"followUpQuestions\":[\"Can you restate it?\"]}");
            return null;
        }).when(aiClient).stream(anyString(), any());

        service.send(UUID.randomUUID(), 10L, 20L, new SendMessageRequest(AssistantMode.EXPLAIN, "Explain", null, null));

        verify(persistence).complete(3L, "First second\n\nCan you restate it?");
        verify(persistence, never()).fail(anyLong());
    }

    @Test
    void invalidStructuredResponseIsRetriedBeforePersistence() {
        when(persistence.prepare(any(), eq(10L), eq(20L), any())).thenReturn(new PreparedMessage(context, List.of()));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("{\"teachingPoints\":[\"First?\"],\"examples\":[],\"followUpQuestions\":[\"Ready?\"]}");
            return null;
        }).doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("{\"teachingPoints\":[\"Use the definition first.\"],\"examples\":[],\"followUpQuestions\":[\"Which definition applies?\"]}");
            return null;
        }).when(aiClient).stream(anyString(), any());

        service.send(UUID.randomUUID(), 10L, 20L,
                new SendMessageRequest(AssistantMode.EXPLAIN, "Explain", null, null));

        verify(aiClient, times(2)).stream(anyString(), any());
        verify(persistence).complete(3L, "Use the definition first.\n\nWhich definition applies?");
        verify(persistence, never()).fail(anyLong());
    }

    @Test
    void failedProviderMarksPendingMessageFailed() {
        when(persistence.prepare(any(), eq(10L), eq(20L), any())).thenReturn(new PreparedMessage(context, List.of()));
        doThrow(new RuntimeException("provider unavailable")).when(aiClient).stream(anyString(), any());

        service.send(UUID.randomUUID(), 10L, 20L, new SendMessageRequest(AssistantMode.EXPLAIN, "Explain", null, null));

        verify(persistence).fail(3L);
        verify(persistence, never()).complete(anyLong(), anyString());
    }

    @Test
    void clientDisconnectDuringSseDeliveryMarksPendingMessageFailed() throws IOException {
        PreparedMessage prepared = new PreparedMessage(context, List.of());
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("{\"teachingPoints\":[\"First second\"],\"examples\":[],");
            consumer.accept("\"followUpQuestions\":[\"Can you restate it?\"]}");
            return null;
        }).when(aiClient).stream(anyString(), any());
        SseEmitter emitter = mock(SseEmitter.class);
        doNothing().doThrow(new IOException("client disconnected"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        service.stream(prepared, emitter);

        verify(persistence).fail(3L);
        verify(persistence, never()).complete(anyLong(), anyString());
        verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
        verify(emitter, never()).completeWithError(any());
    }

    @Test
    void closedStreamBeforeStartDoesNotRequestAnAnswer() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IllegalStateException("stream already closed"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        service.stream(new PreparedMessage(context, List.of()), emitter);

        verifyNoInteractions(aiClient);
        verify(persistence).fail(3L);
        verify(persistence, never()).complete(anyLong(), anyString());
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
        verify(emitter, never()).completeWithError(any());
    }

    @Test
    void disconnectWhileReportingProviderFailureDoesNotSendAgain() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        doNothing().doThrow(new IOException("client disconnected"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        doThrow(new RuntimeException("provider unavailable")).when(aiClient).stream(anyString(), any());

        service.stream(new PreparedMessage(context, List.of()), emitter);

        verify(persistence).fail(3L);
        verify(persistence, never()).complete(anyLong(), anyString());
        verify(aiClient).stream(anyString(), any());
        verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
        verify(emitter, never()).completeWithError(any());
    }

    @Test
    void disconnectAtFinalEventDoesNotResendCompletedAnswer() throws IOException {
        SseEmitter emitter = mock(SseEmitter.class);
        doNothing().doNothing().doThrow(new IOException("client disconnected"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        doAnswer(invocation -> {
            java.util.function.Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("{\"teachingPoints\":[\"One point.\"],\"examples\":[],\"followUpQuestions\":[\"Ready?\"]}");
            return null;
        }).when(aiClient).stream(anyString(), any());

        service.stream(new PreparedMessage(context, List.of()), emitter);

        verify(persistence).complete(3L, "One point.\n\nReady?");
        verify(aiClient).stream(anyString(), any());
        verify(emitter, times(3)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter, never()).complete();
        verify(emitter, never()).completeWithError(any());
    }
}
