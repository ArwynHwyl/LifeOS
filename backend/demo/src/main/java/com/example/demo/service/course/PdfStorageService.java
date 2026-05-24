package com.example.demo.service.course;

import com.example.demo.config.S3StorageProperties;
import com.example.demo.service.exception.ValidationException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
class PdfStorageService {

    private final S3StorageProperties properties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    PdfStorageService(S3StorageProperties properties, S3Client s3Client, S3Presigner s3Presigner) {
        this.properties = properties;
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    PresignedUpload presignUpload(String storagePath, String contentType, Duration ttl) {
        requireConfigured();
        Instant expiresAt = Instant.now().plus(ttl);
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket().trim())
                .key(storagePath)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(objectRequest)
                .build();
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return new PresignedUpload(
                presigned.url().toString(),
                storagePath,
                expiresAt,
                "PUT",
                Map.of("Content-Type", contentType)
        );
    }

    PresignedRead presignRead(String storagePath, Duration ttl) {
        requireConfigured();
        Instant expiresAt = Instant.now().plus(ttl);
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket().trim())
                .key(storagePath)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(objectRequest)
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return new PresignedRead(presigned.url().toString(), expiresAt);
    }

    Optional<ObjectMetadata> metadata(String storagePath) {
        requireConfigured();
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket().trim())
                    .key(storagePath)
                    .build());
            return Optional.of(new ObjectMetadata(response.contentLength(), response.contentType()));
        } catch (NoSuchKeyException ex) {
            return Optional.empty();
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    InputStream download(String storagePath) {
        requireConfigured();
        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                .bucket(properties.bucket().trim())
                .key(storagePath)
                .build());
        return response;
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new ValidationException("PDF storage is not configured");
        }
    }

    record PresignedUpload(
            String uploadUrl,
            String storagePath,
            Instant expiresAt,
            String method,
            Map<String, String> headers
    ) {
    }

    record PresignedRead(String fileUrl, Instant expiresAt) {
    }

    record ObjectMetadata(Long contentLength, String contentType) {
    }
}
