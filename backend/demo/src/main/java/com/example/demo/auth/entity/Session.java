package com.example.demo.auth.entity;

import com.example.demo.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "access_token", nullable = false, length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, unique = true, length = 1000)
    private String refreshToken;

    @Column(name = "expire_at", nullable = false)
    private Instant expireAt;

    @Column(name = "is_revoke", nullable = false)
    private boolean revoke;

    protected Session() {
    }

    public Session(User user, String accessToken, String refreshToken, Instant expireAt) {
        this.user = user;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expireAt = expireAt;
        this.revoke = false;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public User getUser() {
        return user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    public boolean isRevoke() {
        return revoke;
    }

    public void rotateTokens(String accessToken, String refreshToken, Instant expireAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expireAt = expireAt;
    }

    public void revoke() {
        this.revoke = true;
    }
}
