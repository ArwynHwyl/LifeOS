package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.exception.AccessDeniedException;
import java.util.UUID;

final class CurrentUser {

    private CurrentUser() {
    }

    static UUID id(User user) {
        if (user == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        return user.getUserId();
    }
}
