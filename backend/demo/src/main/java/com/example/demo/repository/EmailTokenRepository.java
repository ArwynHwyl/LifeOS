package com.example.demo.repository;

import com.example.demo.entity.EmailToken;
import com.example.demo.entity.EmailTokenType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {

    Optional<EmailToken> findByTokenHashAndType(String tokenHash, EmailTokenType type);
}
