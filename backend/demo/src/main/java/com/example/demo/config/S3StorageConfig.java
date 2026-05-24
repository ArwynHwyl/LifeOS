package com.example.demo.config;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3StorageConfig {

    @Bean
    S3Client s3Client(S3StorageProperties properties) {
        var builder = S3Client.builder()
                .region(Region.of(properties.normalizedRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .serviceConfiguration(s3Configuration());
        if (!isBlank(properties.endpoint())) {
            builder.endpointOverride(URI.create(properties.endpoint().trim()));
        }
        return builder.build();
    }

    @Bean
    S3Presigner s3Presigner(S3StorageProperties properties) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.normalizedRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .serviceConfiguration(s3Configuration());
        if (!isBlank(properties.endpoint())) {
            builder.endpointOverride(URI.create(properties.endpoint().trim()));
        }
        return builder.build();
    }

    private StaticCredentialsProvider credentialsProvider(S3StorageProperties properties) {
        String accessKey = isBlank(properties.accessKey()) ? "missing-access-key" : properties.accessKey().trim();
        String secretKey = isBlank(properties.secretKey()) ? "missing-secret-key" : properties.secretKey().trim();
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private S3Configuration s3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
