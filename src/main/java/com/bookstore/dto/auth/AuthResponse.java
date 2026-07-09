package com.bookstore.dto.auth;

import com.bookstore.entity.Role;

import java.util.UUID;

public record AuthResponse(
        UserSummary user,
        String accessToken,
        String refreshToken
) {
    public record UserSummary(UUID id, String name, String email, Role role) {
    }
}
