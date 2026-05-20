package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_tokens")
public class EmailToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "email_token_id", nullable = false, updatable = false)
    private UUID emailTokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmailTokenType type;

    @Column(name = "expire_at", nullable = false)
    private Instant expireAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "create_at", nullable = false, updatable = false)
    private Instant createAt;

    protected EmailToken() {
    }

    public EmailToken(User user, String tokenHash, EmailTokenType type, Instant expireAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.type = type;
        this.expireAt = expireAt;
    }

    @PrePersist
    void prePersist() {
        this.createAt = Instant.now();
    }

    public User getUser() {
        return user;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }
}
