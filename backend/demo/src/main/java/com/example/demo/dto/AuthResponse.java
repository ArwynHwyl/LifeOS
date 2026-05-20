package com.example.demo.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Instant expireAt,
        UserResponse user
) {
}
