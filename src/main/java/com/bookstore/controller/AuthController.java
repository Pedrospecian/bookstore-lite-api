package com.bookstore.controller;

import com.bookstore.dto.auth.AuthResponse;
import com.bookstore.dto.auth.LoginRequest;
import com.bookstore.dto.auth.RefreshRequest;
import com.bookstore.dto.auth.RegisterRequest;
import com.bookstore.entity.User;
import com.bookstore.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserSummary> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new AuthResponse.UserSummary(user.getId(), user.getName(), user.getEmail(), user.getRole()));
    }
}
