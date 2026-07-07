package com.example.demo.shared.security;

import com.example.demo.user.entity.User;
import com.example.demo.shared.exception.AccessDeniedException;
import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        return user.getUserId();
    }
}
