package com.example.demo.service.course;

import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.exception.AccessDeniedException;
import com.example.demo.service.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class UserAccessService {

    private final UserRepository userRepository;

    UserAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    User requireAdmin(UUID userId) {
        return requireRole(userId, UserRole.ROLE_ADMIN);
    }

    User requireTeacher(UUID userId) {
        return requireRole(userId, UserRole.ROLE_TEACHER);
    }

    private User requireRole(UUID userId, UserRole role) {
        if (userId == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getRole() != role) {
            throw new AccessDeniedException("User " + userId + " must have role " + role);
        }
        return user;
    }
}
