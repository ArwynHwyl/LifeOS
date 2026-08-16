package com.example.demo.learningassistant.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.demo.course.entity.*;
import com.example.demo.course.repository.CourseRepository;
import com.example.demo.learningassistant.dto.AssistantDtos.*;
import com.example.demo.learningassistant.entity.*;
import com.example.demo.learningassistant.repository.*;
import com.example.demo.shared.exception.InvalidWorkflowStateException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.shared.exception.ValidationException;
import com.example.demo.user.entity.*;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AssistantPersistenceServiceTests {
    @Mock CourseRepository courseRepository;
    @Mock AssistantConversationRepository conversationRepository;
    @Mock AssistantMessageRepository messageRepository;
    @Mock EntityManager entityManager;
    AssistantPersistenceService service;
    User learner;
    Course course;
    SubTopic subTopic;

    @BeforeEach
    void setUp() {
        service = new AssistantPersistenceService(courseRepository, conversationRepository, messageRepository, entityManager);
        learner = new User("learner@example.com", "learner", "hash", "Learn", "Er", UserRole.ROLE_LEARNER, UserStatus.VERIFY);
        CourseModule module = new CourseModule("Module", "Description", 1, ContentDepth.MEDIUM);
        subTopic = new SubTopic("Sets", "<p>A set is a collection. The empty set has no members.</p>", 1,
                SubTopicSourceType.MANUAL, null, null);
        module.addSubTopic(subTopic);
        course = new Course("Discrete Math", "Description", learner);
        course.addModule(module);
        course.publish();
        ReflectionTestUtils.setField(course, "id", 10L);
        ReflectionTestUtils.setField(module, "id", 20L);
        ReflectionTestUtils.setField(subTopic, "id", 30L);
    }

    @Test
    void prepareReusesConversationAndPersistsMessagePair() {
        AssistantConversation conversation = conversation(learner);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(conversationRepository.findByUserUserIdAndSubTopicId(learner.getUserId(), 30L))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findTop12ByConversationIdAndStatusOrderByCreatedAtDescIdDesc(
                40L, AssistantMessageStatus.COMPLETED)).thenReturn(List.of());
        AtomicLong ids = new AtomicLong(50);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            AssistantMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", ids.getAndIncrement());
            return message;
        });

        var prepared = service.prepare(learner.getUserId(), 10L, 30L,
                new SendMessageRequest(AssistantMode.EXPLAIN, "Explain this", null, "empty set"));

        assertThat(prepared.context().conversationId()).isEqualTo(40L);
        assertThat(prepared.context().selectedText()).isEqualTo("empty set");
        assertThat(prepared.context().assistantMessageId()).isEqualTo(51L);
        verify(messageRepository, times(2)).save(any());
    }

    @Test
    void openReusesOwnedConversationAndReturnsRepositoryOrder() {
        AssistantConversation conversation = conversation(learner);
        AssistantMessage first = message(conversation, AssistantMessageRole.USER, "First", 1L,
                Instant.parse("2026-01-01T00:00:00Z"));
        AssistantMessage second = message(conversation, AssistantMessageRole.ASSISTANT, "Second", 2L,
                Instant.parse("2026-01-01T00:00:01Z"));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(conversationRepository.findByUserUserIdAndSubTopicId(learner.getUserId(), 30L))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(40L))
                .thenReturn(List.of(first, second));

        ConversationDto result = service.open(learner.getUserId(), 10L, 30L);

        assertThat(result.conversationId()).isEqualTo(40L);
        assertThat(result.context().subTopicTitle()).isEqualTo("Sets");
        assertThat(result.suggestions()).hasSize(5);
        assertThat(result.messages()).extracting(MessageDto::content).containsExactly("First", "Second");
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void openCreatesAnEmptyConversationWhenNoneExists() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(conversationRepository.findByUserUserIdAndSubTopicId(learner.getUserId(), 30L))
                .thenReturn(Optional.empty());
        when(entityManager.getReference(User.class, learner.getUserId())).thenReturn(learner);
        when(conversationRepository.save(any())).thenAnswer(invocation -> {
            AssistantConversation saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 41L);
            return saved;
        });
        when(messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(41L)).thenReturn(List.of());

        ConversationDto result = service.open(learner.getUserId(), 10L, 30L);

        assertThat(result.conversationId()).isEqualTo(41L);
        assertThat(result.messages()).isEmpty();
        verify(entityManager).getReference(User.class, learner.getUserId());
    }

    @Test
    void submittedSuggestionCanContainLearnerEditedText() {
        AssistantConversation conversation = conversation(learner);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(conversationRepository.findByUserUserIdAndSubTopicId(learner.getUserId(), 30L))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findTop12ByConversationIdAndStatusOrderByCreatedAtDescIdDesc(
                40L, AssistantMessageStatus.COMPLETED)).thenReturn(List.of());
        AtomicLong ids = new AtomicLong(70);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            AssistantMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", ids.getAndIncrement());
            return message;
        });

        var prepared = service.prepare(learner.getUserId(), 10L, 30L,
                new SendMessageRequest(AssistantMode.EXPLAIN,
                        "Explain this section more simply, especially the empty set.",
                        "EXPLAIN_SIMPLY", null));

        assertThat(prepared.context().message())
                .isEqualTo("Explain this section more simply, especially the empty set.");
        ArgumentCaptor<AssistantMessage> messageCaptor = ArgumentCaptor.forClass(AssistantMessage.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        AssistantMessage userMessage = messageCaptor.getAllValues().get(0);
        assertThat(userMessage.getSuggestionKey()).isEqualTo("EXPLAIN_SIMPLY");
        assertThat(userMessage.getContent()).isEqualTo(prepared.context().message());
    }

    @Test
    void selectedTextMustExistInCurrentLesson() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.prepare(learner.getUserId(), 10L, 30L,
                new SendMessageRequest(AssistantMode.EXPLAIN, "Explain", null, "not in the lesson")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("selectedText");
        verifyNoInteractions(conversationRepository, messageRepository);
    }

    @Test
    void feedbackDoesNotRevealAMessageOwnedByAnotherUser() {
        when(messageRepository.findByIdAndConversationUserUserId(60L, learner.getUserId()))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.feedback(learner.getUserId(), 60L,
                new FeedbackRequest(AssistantFeedback.HELPFUL)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Assistant message not found: 60");
        verify(messageRepository, never()).findById(60L);
    }

    @Test
    void feedbackStoresHelpfulAndCanBeChangedWithoutCreatingARecord() {
        AssistantMessage answer = message(conversation(learner), AssistantMessageRole.ASSISTANT,
                "Answer", 60L, Instant.parse("2026-01-02T00:00:01Z"));
        when(messageRepository.findByIdAndConversationUserUserId(60L, learner.getUserId()))
                .thenReturn(Optional.of(answer));

        FeedbackResponse first = service.feedback(learner.getUserId(), 60L,
                new FeedbackRequest(AssistantFeedback.HELPFUL));
        FeedbackResponse repeated = service.feedback(learner.getUserId(), 60L,
                new FeedbackRequest(AssistantFeedback.HELPFUL));
        FeedbackResponse changed = service.feedback(learner.getUserId(), 60L,
                new FeedbackRequest(AssistantFeedback.NOT_UNDERSTOOD));

        assertThat(first.feedback()).isEqualTo(AssistantFeedback.HELPFUL);
        assertThat(repeated.feedback()).isEqualTo(AssistantFeedback.HELPFUL);
        assertThat(changed.feedback()).isEqualTo(AssistantFeedback.NOT_UNDERSTOOD);
        assertThat(answer.getFeedback()).isEqualTo(AssistantFeedback.NOT_UNDERSTOOD);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void feedbackRejectsNonPositiveMessageIdBeforeQuerying() {
        assertThatThrownBy(() -> service.feedback(learner.getUserId(), 0L,
                new FeedbackRequest(AssistantFeedback.HELPFUL)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("messageId must be positive");
        verifyNoInteractions(messageRepository);
    }

    @Test
    void feedbackRejectsLearnerAndIncompleteMessages() {
        AssistantConversation conversation = conversation(learner);
        AssistantMessage learnerMessage = message(conversation, AssistantMessageRole.USER,
                "Question", 61L, Instant.parse("2026-01-02T00:00:01Z"));
        AssistantMessage pendingAnswer = new AssistantMessage(conversation, AssistantMessageRole.ASSISTANT,
                AssistantMode.EXPLAIN, "", null, null, AssistantMessageStatus.PENDING);
        ReflectionTestUtils.setField(pendingAnswer, "id", 62L);
        when(messageRepository.findByIdAndConversationUserUserId(61L, learner.getUserId()))
                .thenReturn(Optional.of(learnerMessage));
        when(messageRepository.findByIdAndConversationUserUserId(62L, learner.getUserId()))
                .thenReturn(Optional.of(pendingAnswer));

        assertThatThrownBy(() -> service.feedback(learner.getUserId(), 61L,
                new FeedbackRequest(AssistantFeedback.HELPFUL)))
                .isInstanceOf(InvalidWorkflowStateException.class);
        assertThatThrownBy(() -> service.feedback(learner.getUserId(), 62L,
                new FeedbackRequest(AssistantFeedback.HELPFUL)))
                .isInstanceOf(InvalidWorkflowStateException.class);
    }

    @Test
    void weaknessesIncludeOnlyRepeatedTopicsAndCountNotUnderstood() {
        AssistantConversation conversation = conversation(learner);
        AssistantMessage first = message(conversation, AssistantMessageRole.USER, "Question 1", 1L, Instant.parse("2026-01-01T00:00:00Z"));
        AssistantMessage second = message(conversation, AssistantMessageRole.USER, "Question 2", 2L, Instant.parse("2026-01-02T00:00:00Z"));
        AssistantMessage answer = message(conversation, AssistantMessageRole.ASSISTANT, "Answer", 3L, Instant.parse("2026-01-02T00:00:01Z"));
        answer.updateFeedback(AssistantFeedback.NOT_UNDERSTOOD);
        when(conversationRepository.findByUserUserId(learner.getUserId())).thenReturn(List.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(40L))
                .thenReturn(List.of(first, second, answer));

        List<WeaknessDto> result = service.weaknesses(learner.getUserId());

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.subTopicId()).isEqualTo(30L);
            assertThat(item.questionCount()).isEqualTo(2);
            assertThat(item.notUnderstoodCount()).isEqualTo(1);
        });
        verify(conversationRepository).findByUserUserId(learner.getUserId());
    }

    private AssistantConversation conversation(User user) {
        AssistantConversation value = new AssistantConversation(user, course, subTopic);
        ReflectionTestUtils.setField(value, "id", 40L);
        return value;
    }

    private AssistantMessage message(AssistantConversation conversation, AssistantMessageRole role,
            String content, Long id, Instant createdAt) {
        AssistantMessage value = new AssistantMessage(conversation, role, AssistantMode.EXPLAIN, content,
                null, null, AssistantMessageStatus.COMPLETED);
        ReflectionTestUtils.setField(value, "id", id);
        ReflectionTestUtils.setField(value, "createdAt", createdAt);
        return value;
    }
}
