package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @NotBlank String token
) {
}
