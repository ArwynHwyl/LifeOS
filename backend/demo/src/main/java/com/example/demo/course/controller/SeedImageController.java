package com.example.demo.course.controller;

import com.example.demo.course.service.interactive.SeedImageStorage;
import java.time.Duration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seed-images")
public class SeedImageController {

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> seedImage(@PathVariable String fileName) {
        return SeedImageStorage.resource(fileName)
                .map(resource -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(SeedImageStorage.contentType(fileName)))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                        .body(resource))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
