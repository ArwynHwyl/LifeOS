package com.example.demo.learningassistant.entity;

import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.SubTopic;
import com.example.demo.shared.entity.BaseTimestampEntity;
import com.example.demo.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "assistant_conversation", uniqueConstraints =
        @UniqueConstraint(name = "uk_assistant_conversation_user_subtopic", columnNames = {"user_id", "sub_topic_id"}))
public class AssistantConversation extends BaseTimestampEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_topic_id", nullable = false)
    private SubTopic subTopic;

    protected AssistantConversation() {}

    public AssistantConversation(User user, Course course, SubTopic subTopic) {
        this.user = user;
        this.course = course;
        this.subTopic = subTopic;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Course getCourse() { return course; }
    public SubTopic getSubTopic() { return subTopic; }
}
