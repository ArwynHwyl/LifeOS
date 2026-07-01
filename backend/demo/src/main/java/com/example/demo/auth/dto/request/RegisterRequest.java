package com.example.demo.auth.dto.request;

import com.example.demo.user.entity.UserRole;
import com.example.demo.shared.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(max = 100) @StrongPassword String password,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        UserRole role
) {
}
