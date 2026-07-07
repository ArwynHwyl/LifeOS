package com.example.demo.course.service.learner;

import com.example.demo.user.entity.User;
import com.example.demo.user.entity.UserRole;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.shared.exception.AccessDeniedException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserAccessService {

    private final UserRepository userRepository;

public UserAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

public User requireAdmin(UUID userId) {
        return requireRole(userId, UserRole.ROLE_ADMIN);
    }

public User requireTeacher(UUID userId) {
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
