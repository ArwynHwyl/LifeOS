package com.example.demo.auth.repository;

import com.example.demo.auth.entity.Session;
import com.example.demo.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByRefreshTokenAndRevokeFalse(String refreshToken);

    Iterable<Session> findByUserAndRevokeFalse(User user);
}
