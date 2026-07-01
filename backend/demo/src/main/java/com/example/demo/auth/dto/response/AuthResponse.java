package com.example.demo.auth.dto.response;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Instant expireAt,
        UserResponse user
) {
}
