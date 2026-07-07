package com.example.demo.auth.dto.request;

import com.example.demo.shared.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 100) @StrongPassword String newPassword
) {
}
