package com.example.demo.service;

import com.example.demo.entity.EmailToken;
import com.example.demo.entity.EmailTokenType;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.EmailTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EmailTokenService {

    private final EmailTokenRepository emailTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailTokenService(EmailTokenRepository emailTokenRepository) {
        this.emailTokenRepository = emailTokenRepository;
    }

    public String createToken(User user, EmailTokenType type, Duration ttl) {
        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);
        emailTokenRepository.save(new EmailToken(user, tokenHash, type, Instant.now().plus(ttl)));
        return rawToken;
    }

    public EmailToken consumeToken(String rawToken, EmailTokenType type) {
        EmailToken token = emailTokenRepository.findByTokenHashAndType(hash(rawToken), type)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Token is invalid or expired"));

        if (token.getUsedAt() != null || token.getExpireAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token is invalid or expired");
        }

        token.markUsed();
        return token;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
