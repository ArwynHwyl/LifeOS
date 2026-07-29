package com.example.demo.learningassistant.service;

import static org.mockito.Mockito.*;

import com.example.demo.learningassistant.entity.AssistantMode;
import com.example.demo.learningassistant.dto.AssistantDtos.SendMessageRequest;
import com.example.demo.learningassistant.service.AssistantPersistenceService.PreparedMessage;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        Executor directExecutor = Runnable::run;
        service = new LearningAssistantService(persistence, promptBuilder, aiClient, directExecutor);
        context = new AssistantStreamContext(1L, 2L, 3L, AssistantMode.EXPLAIN, "Explain", null,
                "Course", "Module", "Topic", "Lesson", null, null, null);
    }

    @Test
    void completedStreamPersistsCombinedAnswer() {
        when(persistence.prepare(any(), eq(10L), eq(20L), any())).thenReturn(new PreparedMessage(context, List.of()));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("First ");
            consumer.accept("second");
            return null;
        }).when(aiClient).stream(anyString(), any());

        service.send(UUID.randomUUID(), 10L, 20L, new SendMessageRequest(AssistantMode.EXPLAIN, "Explain", null, null));

        verify(persistence).complete(3L, "First second");
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
}
