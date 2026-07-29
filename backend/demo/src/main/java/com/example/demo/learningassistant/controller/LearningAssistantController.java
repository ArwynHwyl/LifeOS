package com.example.demo.learningassistant.controller;

import com.example.demo.learningassistant.dto.AssistantDtos.*;
import com.example.demo.learningassistant.service.LearningAssistantService;
import com.example.demo.shared.security.CurrentUser;
import com.example.demo.user.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@PreAuthorize("hasAnyRole('LEARNER', 'TEACHER', 'ADMIN')")
public class LearningAssistantController {
    private final LearningAssistantService service;

    public LearningAssistantController(LearningAssistantService service) { this.service = service; }

    @GetMapping("/api/v1/learner/courses/{courseId}/subtopics/{subTopicId}/assistant")
    public ConversationDto open(@AuthenticationPrincipal User user, @PathVariable Long courseId,
            @PathVariable Long subTopicId) {
        return service.open(CurrentUser.id(user), courseId, subTopicId);
    }

    @PostMapping(value = "/api/v1/learner/courses/{courseId}/subtopics/{subTopicId}/assistant/messages",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter send(@AuthenticationPrincipal User user, @PathVariable Long courseId,
            @PathVariable Long subTopicId, @Valid @RequestBody SendMessageRequest request) {
        return service.send(CurrentUser.id(user), courseId, subTopicId, request);
    }

    @PutMapping("/api/v1/learner/assistant/messages/{messageId}/feedback")
    public FeedbackResponse feedback(@AuthenticationPrincipal User user, @PathVariable Long messageId,
            @Valid @RequestBody FeedbackRequest request) {
        return service.feedback(CurrentUser.id(user), messageId, request);
    }

    @GetMapping("/api/v1/learner/assistant/weaknesses")
    public List<WeaknessDto> weaknesses(@AuthenticationPrincipal User user) {
        return service.weaknesses(CurrentUser.id(user));
    }
}
