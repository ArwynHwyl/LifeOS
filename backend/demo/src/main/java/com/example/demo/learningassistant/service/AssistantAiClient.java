package com.example.demo.learningassistant.service;

import java.util.function.Consumer;

public interface AssistantAiClient {
    void stream(String prompt, Consumer<String> deltaConsumer);
}
