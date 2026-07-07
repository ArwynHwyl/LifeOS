package com.example.demo.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.gemini")
public record GeminiAiProperties(
        String apiKey,
        String model
) {

    public String normalizedModel() {
        return model == null || model.isBlank() ? "gemini-2.5-flash" : model.trim();
    }
}
