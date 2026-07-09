package com.bookstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters-long";

    private JwtService newService(long accessExpMs, long refreshExpMs) {
        return new JwtService(SECRET, accessExpMs, refreshExpMs);
    }

    @Test
    void generateAccessTokenCarregaSubjectERole() {
        JwtService service = newService(900_000, 604_800_000);
        String userId = UUID.randomUUID().toString();

        String token = service.generateAccessToken(userId, "ADMIN");
        Claims claims = service.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(service.isRefreshToken(claims)).isFalse();
    }

    @Test
    void generateRefreshTokenNaoCarregaRole() {
        JwtService service = newService(900_000, 604_800_000);
        String userId = UUID.randomUUID().toString();

        String token = service.generateRefreshToken(userId);
        Claims claims = service.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId);
        assertThat(claims.get("role")).isNull();
        assertThat(service.isRefreshToken(claims)).isTrue();
    }

    @Test
    void parseClaimsLancaExcecaoParaTokenExpirado() throws InterruptedException {
        // expiração de 1ms — o token já nasce expirado por segurança do teste
        JwtService service = newService(1, 604_800_000);
        String token = service.generateAccessToken(UUID.randomUUID().toString(), "CUSTOMER");

        Thread.sleep(20);

        assertThatThrownBy(() -> service.parseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parseClaimsLancaExcecaoParaTokenAssinadoComOutroSegredo() {
        JwtService serviceA = newService(900_000, 604_800_000);
        JwtService serviceB = new JwtService(
                "outro-segredo-completamente-diferente-32chars", 900_000, 604_800_000
        );

        String token = serviceA.generateAccessToken(UUID.randomUUID().toString(), "CUSTOMER");

        assertThatThrownBy(() -> serviceB.parseClaims(token))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
