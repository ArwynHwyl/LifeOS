package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.EmailRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.EmailToken;
import com.example.demo.entity.EmailTokenType;
import com.example.demo.entity.Session;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.entity.UserStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailTokenService emailTokenService;
    private final EmailDeliveryService emailDeliveryService;

    public AuthService(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            EmailTokenService emailTokenService,
            EmailDeliveryService emailDeliveryService
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailTokenService = emailTokenService;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserRole role = request.role() == null ? UserRole.ROLE_LEARNER : request.role();
        User user = new User(
                request.email().toLowerCase(),
                request.username(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                role
        );
        User savedUser = userRepository.save(user);
        sendEmailConfirmation(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
            );
        } catch (BadCredentialsException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (user.getStatus() != UserStatus.VERIFY) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Please verify your email before logging in");
        }
        return createSession(user);
    }

    @Transactional
    public void requestEmailConfirmation(EmailRequest request) {
        userRepository.findByEmail(request.email().toLowerCase())
                .filter(user -> user.getStatus() != UserStatus.VERIFY)
                .ifPresent(this::sendEmailConfirmation);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        EmailToken token = emailTokenService.consumeToken(rawToken, EmailTokenType.EMAIL_CONFIRMATION);
        User user = token.getUser();
        user.verifyEmail();
    }

    @Transactional
    public void forgotPassword(EmailRequest request) {
        userRepository.findByEmail(request.email().toLowerCase()).ifPresent(user -> {
            String token = emailTokenService.createToken(user, EmailTokenType.PASSWORD_RESET, Duration.ofMinutes(30));
            emailDeliveryService.sendPasswordReset(user.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        EmailToken token = emailTokenService.consumeToken(request.token(), EmailTokenType.PASSWORD_RESET);
        User user = token.getUser();
        user.updatePasswordHash(passwordEncoder.encode(request.newPassword()));
        revokeSessions(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims = parseRefreshToken(request.refreshToken());
        Session session = sessionRepository.findByRefreshTokenAndRevokeFalse(request.refreshToken())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid"));

        if (session.getExpireAt().isBefore(Instant.now())) {
            session.revoke();
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is expired");
        }

        if (!session.getUser().getUserId().toString().equals(claims.getSubject())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }

        User user = session.getUser();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        Instant refreshExpiresAt = jwtService.refreshTokenExpiry();
        session.rotateTokens(accessToken, refreshToken, refreshExpiresAt);

        return new AuthResponse(accessToken, refreshToken, refreshExpiresAt, UserResponse.from(user));
    }

    @Transactional
    public void logout(String refreshToken) {
        sessionRepository.findByRefreshTokenAndRevokeFalse(refreshToken).ifPresent(Session::revoke);
    }

    private AuthResponse createSession(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        Instant refreshExpiresAt = jwtService.refreshTokenExpiry();
        sessionRepository.save(new Session(user, accessToken, refreshToken, refreshExpiresAt));
        return new AuthResponse(accessToken, refreshToken, refreshExpiresAt, UserResponse.from(user));
    }

    private void sendEmailConfirmation(User user) {
        String token = emailTokenService.createToken(user, EmailTokenType.EMAIL_CONFIRMATION, Duration.ofHours(24));
        emailDeliveryService.sendEmailConfirmation(user.getEmail(), token);
    }

    private void revokeSessions(User user) {
        for (Session session : sessionRepository.findByUserAndRevokeFalse(user)) {
            session.revoke();
        }
    }

    private Claims parseRefreshToken(String refreshToken) {
        try {
            Claims claims = jwtService.parse(refreshToken);
            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
        }
    }
}
