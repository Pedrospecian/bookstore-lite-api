package com.bookstore.service;

import com.bookstore.dto.auth.AuthResponse;
import com.bookstore.dto.auth.LoginRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.exception.DuplicateResourceException;
import com.bookstore.exception.InvalidCredentialsException;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Refresh token inválido ou expirado");
        }

        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidCredentialsException("Token informado não é um refresh token");
        }

        User user = userRepository.findById(UUID.fromString(claims.getSubject()))
                .orElseThrow(() -> new InvalidCredentialsException("Usuário não encontrado"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId().toString(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId().toString());

        return new AuthResponse(
                new AuthResponse.UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole()),
                accessToken,
                refreshToken
        );
    }
}
