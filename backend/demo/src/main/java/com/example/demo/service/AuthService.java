package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RefreshTokenRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.Session;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.SessionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

    public AuthService(
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
        return createSession(savedUser);
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
        return createSession(user);
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
