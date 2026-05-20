package com.example.demo.dto;

import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.entity.UserStatus;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String email,
        String username,
        String firstName,
        String lastName,
        UserStatus status,
        UserRole role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                user.getRole()
        );
    }
}
