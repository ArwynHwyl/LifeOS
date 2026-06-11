package com.example.demo.dto;

import com.example.demo.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 100) @StrongPassword String newPassword
) {
}
