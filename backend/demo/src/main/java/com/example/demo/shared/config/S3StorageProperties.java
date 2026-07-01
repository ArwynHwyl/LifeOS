package com.example.demo.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
public record S3StorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey
) {

    public String normalizedRegion() {
        return isBlank(region) ? "us-east-1" : region.trim();
    }

    public boolean isConfigured() {
        return !isBlank(endpoint) && !isBlank(bucket) && !isBlank(accessKey) && !isBlank(secretKey);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
