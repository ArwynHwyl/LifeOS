package com.example.demo.learningassistant.service;

import com.example.demo.course.entity.*;
import com.example.demo.course.repository.CourseRepository;
import com.example.demo.learningassistant.dto.AssistantDtos.*;
import com.example.demo.learningassistant.entity.*;
import com.example.demo.learningassistant.repository.*;
import com.example.demo.shared.exception.*;
import com.example.demo.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantPersistenceService {
    private static final List<SuggestionDto> SUGGESTIONS = List.of(
            new SuggestionDto("EXPLAIN_SIMPLY", "Explain more simply", AssistantMode.EXPLAIN, "Explain this section more simply."),
            new SuggestionDto("GIVE_EXAMPLE", "Give an example", AssistantMode.EXPLAIN, "Give me one example of the idea in this section."),
            new SuggestionDto("SUMMARIZE", "Summarize this", AssistantMode.EXPLAIN, "Summarize the key idea in this section."),
            new SuggestionDto("CHECK_UNDERSTANDING", "Check my understanding", AssistantMode.EXPLAIN, "Ask me one short question to check my understanding."),
            new SuggestionDto("GIVE_HINT", "Give me a hint", AssistantMode.HINT, "Give me only the next hint without revealing the answer.")
    );

    private final CourseRepository courseRepository;
    private final AssistantConversationRepository conversationRepository;
    private final AssistantMessageRepository messageRepository;
    private final EntityManager entityManager;

    public AssistantPersistenceService(CourseRepository courseRepository,
            AssistantConversationRepository conversationRepository, AssistantMessageRepository messageRepository,
            EntityManager entityManager) {
        this.courseRepository = courseRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public ConversationDto open(UUID userId, Long courseId, Long subTopicId) {
        Lesson lesson = findLesson(courseId, subTopicId);
        AssistantConversation conversation = findOrCreate(userId, lesson.course(), lesson.subTopic());
        List<MessageDto> messages = messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(conversation.getId())
                .stream().map(this::toDto).toList();
        return new ConversationDto(conversation.getId(), greeting(lesson.subTopic()), context(lesson), SUGGESTIONS, messages);
    }

    @Transactional
    public PreparedMessage prepare(UUID userId, Long courseId, Long subTopicId, SendMessageRequest request) {
        if (request == null || request.mode() == null) throw new ValidationException("mode is required");
        Lesson lesson = findLesson(courseId, subTopicId);
        String suggestionMessage = resolveSuggestion(request.suggestionKey(), request.mode());
        String message = trimToNull(request.message());
        if (message == null) message = suggestionMessage;
        if (message == null) throw new ValidationException("message or suggestionKey is required");
        if (message.length() > 1000) throw new ValidationException("message must not exceed 1000 characters");
        String selectedText = validateSelectedText(request.selectedText(), lesson.subTopic().getContent());
        AssistantConversation conversation = findOrCreate(userId, lesson.course(), lesson.subTopic());
        List<AssistantMessage> history = new ArrayList<>(messageRepository
                .findTop12ByConversationIdAndStatusOrderByCreatedAtDescIdDesc(
                        conversation.getId(), AssistantMessageStatus.COMPLETED));
        Collections.reverse(history);
        AssistantMessage userMessage = messageRepository.save(new AssistantMessage(conversation,
                AssistantMessageRole.USER, request.mode(), message, selectedText, request.suggestionKey(),
                AssistantMessageStatus.COMPLETED));
        AssistantMessage assistantMessage = messageRepository.save(new AssistantMessage(conversation,
                AssistantMessageRole.ASSISTANT, request.mode(), "", null, null, AssistantMessageStatus.PENDING));
        AssistantStreamContext streamContext = new AssistantStreamContext(conversation.getId(), userMessage.getId(),
                assistantMessage.getId(), request.mode(), message, selectedText, lesson.course().getTitle(),
                lesson.module().getTitle(), lesson.subTopic().getTitle(), plainText(lesson.subTopic().getContent()),
                lesson.subTopic().getMascotPrompt(), lesson.subTopic().getInteractionPrompt(),
                lesson.subTopic().getInteractionConfig());
        return new PreparedMessage(streamContext, history);
    }

    @Transactional
    public void complete(Long messageId, String content) {
        AssistantMessage message = findMessage(messageId);
        message.complete(content);
    }

    @Transactional
    public void fail(Long messageId) {
        AssistantMessage message = findMessage(messageId);
        if (message.getStatus() == AssistantMessageStatus.PENDING) message.fail();
    }

    @Transactional
    public FeedbackResponse feedback(UUID userId, Long messageId, FeedbackRequest request) {
        if (request == null || request.feedback() == null) throw new ValidationException("feedback is required");
        AssistantMessage message = findMessage(messageId);
        if (!message.getConversation().getUser().getUserId().equals(userId))
            throw new AccessDeniedException("Assistant message does not belong to the current user");
        if (message.getRole() != AssistantMessageRole.ASSISTANT || message.getStatus() != AssistantMessageStatus.COMPLETED)
            throw new InvalidWorkflowStateException("Feedback is only allowed for completed assistant messages");
        message.updateFeedback(request.feedback());
        return new FeedbackResponse(message.getId(), message.getFeedback());
    }

    @Transactional(readOnly = true)
    public List<WeaknessDto> weaknesses(UUID userId) {
        List<WeaknessDto> result = new ArrayList<>();
        for (AssistantConversation conversation : conversationRepository.findByUserUserId(userId)) {
            List<AssistantMessage> messages = messageRepository
                    .findByConversationIdOrderByCreatedAtAscIdAsc(conversation.getId());
            long questions = messages.stream().filter(m -> m.getRole() == AssistantMessageRole.USER).count();
            if (questions < 2) continue;
            long notUnderstood = messages.stream().filter(m -> m.getRole() == AssistantMessageRole.ASSISTANT
                    && m.getFeedback() == AssistantFeedback.NOT_UNDERSTOOD).count();
            Instant lastAsked = messages.stream().filter(m -> m.getRole() == AssistantMessageRole.USER)
                    .map(AssistantMessage::getCreatedAt).max(Comparator.naturalOrder()).orElse(null);
            SubTopic subTopic = conversation.getSubTopic();
            CourseModule module = subTopic.getModule();
            Course course = conversation.getCourse();
            result.add(new WeaknessDto(course.getId(), course.getTitle(), module.getId(), module.getTitle(),
                    subTopic.getId(), subTopic.getTitle(), questions, notUnderstood, lastAsked));
        }
        result.sort(Comparator.comparingLong(WeaknessDto::questionCount).reversed()
                .thenComparing(Comparator.comparingLong(WeaknessDto::notUnderstoodCount).reversed())
                .thenComparing(WeaknessDto::lastAskedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private Lesson findLesson(Long courseId, Long subTopicId) {
        if (courseId == null || courseId <= 0 || subTopicId == null || subTopicId <= 0)
            throw new ValidationException("courseId and subTopicId must be positive");
        Course course = courseRepository.findById(courseId)
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published course not found: " + courseId));
        for (CourseModule module : course.getModules()) {
            for (SubTopic subTopic : module.getSubTopics()) {
                if (subTopic.getId().equals(subTopicId)) return new Lesson(course, module, subTopic);
            }
        }
        throw new ResourceNotFoundException("Subtopic not found in published course: " + subTopicId);
    }

    private AssistantConversation findOrCreate(UUID userId, Course course, SubTopic subTopic) {
        return conversationRepository.findByUserUserIdAndSubTopicId(userId, subTopic.getId())
                .orElseGet(() -> conversationRepository.save(new AssistantConversation(
                        entityManager.getReference(User.class, userId), course, subTopic)));
    }

    private String resolveSuggestion(String key, AssistantMode mode) {
        if (key == null || key.isBlank()) return null;
        SuggestionDto suggestion = SUGGESTIONS.stream().filter(item -> item.key().equals(key)).findFirst()
                .orElseThrow(() -> new ValidationException("Unknown suggestionKey: " + key));
        if (suggestion.mode() != mode) throw new ValidationException("mode must match suggestionKey");
        return suggestion.message();
    }

    private String validateSelectedText(String selected, String lessonContent) {
        String value = trimToNull(selected);
        if (value == null) return null;
        if (value.length() > 2000) throw new ValidationException("selectedText must not exceed 2000 characters");
        if (!normalize(plainText(lessonContent)).contains(normalize(value)))
            throw new ValidationException("selectedText must come from the current lesson section");
        return value;
    }

    private String normalize(String value) { return value == null ? "" : value.replaceAll("\\s+", " ").trim(); }
    private String plainText(String value) { return value == null ? "" : Jsoup.parse(value).text(); }
    private String trimToNull(String value) { if (value == null || value.trim().isEmpty()) return null; return value.trim(); }
    private AssistantMessage findMessage(Long id) { return messageRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Assistant message not found: " + id)); }
    private String greeting(SubTopic topic) { return topic.getMascotPrompt() == null || topic.getMascotPrompt().isBlank()
            ? "Which part of “" + topic.getTitle() + "” would you like help with?" : topic.getMascotPrompt(); }
    private ContextDto context(Lesson lesson) { return new ContextDto(lesson.course().getId(), lesson.course().getTitle(),
            lesson.module().getId(), lesson.module().getTitle(), lesson.subTopic().getId(), lesson.subTopic().getTitle()); }
    private MessageDto toDto(AssistantMessage message) { return new MessageDto(message.getId(), message.getRole(),
            message.getMode(), message.getContent(), message.getSelectedText(), message.getSuggestionKey(),
            message.getStatus(), message.getFeedback(), message.getCreatedAt()); }

    private record Lesson(Course course, CourseModule module, SubTopic subTopic) {}
    public record PreparedMessage(AssistantStreamContext context, List<AssistantMessage> history) {}
}
