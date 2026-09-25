package com.example.demo.course.service.interactive;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Seed lesson images ship inside the backend jar (resources/seed/images) instead of object storage,
 * so seeded courses render without S3. Their sub-topic assets use a "seed/images/" storage path and
 * are served by {@code SeedImageController}.
 */
public final class SeedImageStorage {

    public static final String STORAGE_PREFIX = "seed/images/";
    public static final String PUBLIC_PATH = "/api/v1/seed-images/";

    private static final Pattern FILE_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*\\.(png|jpe?g|webp)");

    private SeedImageStorage() {
    }

    public static String storagePath(String fileName) {
        return STORAGE_PREFIX + fileName;
    }

    public static boolean isSeedStoragePath(String storagePath) {
        return storagePath != null && storagePath.startsWith(STORAGE_PREFIX);
    }

    public static Optional<Resource> resource(String fileName) {
        if (fileName == null || !FILE_NAME.matcher(fileName).matches()) {
            return Optional.empty();
        }
        Resource resource = new ClassPathResource(storagePath(fileName));
        return resource.exists() ? Optional.of(resource) : Optional.empty();
    }

    public static String contentType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "image/png";
    }

    public static long sizeBytes(String fileName) {
        Resource resource = resource(fileName)
                .orElseThrow(() -> new IllegalArgumentException("Seed image not found: " + fileName));
        try {
            return resource.contentLength();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** Absolute URL when called inside a request (the frontend runs on another origin), relative otherwise. */
    public static String publicUrl(String storagePath) {
        String path = PUBLIC_PATH + storagePath.substring(STORAGE_PREFIX.length());
        if (RequestContextHolder.getRequestAttributes() == null) {
            return path;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(path).toUriString();
    }
}
