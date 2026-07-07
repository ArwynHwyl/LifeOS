package com.example.demo.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @NotBlank String token
) {
}
