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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCriaUsuarioQuandoEmailNaoExiste() {
        var request = new RegisterRequest("Ana Silva", "ana@example.com", "Senha123");

        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha123")).thenReturn("hash-fake");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertThat(response.user().email()).isEqualTo("ana@example.com");
        assertThat(response.user().role()).isEqualTo(Role.CUSTOMER);
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");

        // A senha em texto plano nunca deve ser persistida — só o hash
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("hash-fake")));
    }

    @Test
    void registerLancaErroQuandoEmailJaExiste() {
        var request = new RegisterRequest("Ana", "ana@example.com", "Senha123");
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("E-mail já cadastrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginRetornaTokensParaCredenciaisValidas() {
        var request = new LoginRequest("ana@example.com", "Senha123");
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Ana")
                .email("ana@example.com")
                .passwordHash("hash-armazenado")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Senha123", "hash-armazenado")).thenReturn(true);
        when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
    }

    @Test
    void loginLancaCredenciaisInvalidasQuandoUsuarioNaoExiste() {
        when(userRepository.findByEmail("fantasma@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("fantasma@example.com", "Senha123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginLancaCredenciaisInvalidasQuandoSenhaEstaErrada() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("ana@example.com")
                .passwordHash("hash-armazenado")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SenhaErrada", "hash-armazenado")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@example.com", "SenhaErrada")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshGeraNovosTokensParaTokenValido() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("ana@example.com").role(Role.CUSTOMER).build();

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtService.parseClaims("refresh-valido")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(anyString(), anyString())).thenReturn("novo-access");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("novo-refresh");

        AuthResponse response = authService.refresh("refresh-valido");

        assertThat(response.accessToken()).isEqualTo("novo-access");
    }

    @Test
    void refreshLancaErroQuandoTokenNaoEhDoTipoRefresh() {
        Claims claims = mock(Claims.class);
        when(jwtService.parseClaims("access-usado-como-refresh")).thenReturn(claims);
        when(jwtService.isRefreshToken(claims)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("access-usado-como-refresh"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Token informado não é um refresh token");
    }
}
