package com.bookstore.security;

import com.bookstore.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.parseClaims(token);

            if (jwtService.isRefreshToken(claims)) {
                // Access tokens e refresh tokens não são intercambiáveis
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(claims.getSubject());

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findById(userId).ifPresent(user -> {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // Token inválido ou expirado: segue sem autenticar,
            // o Spring Security barra o acesso a rotas protegidas mais adiante.
        }

        filterChain.doFilter(request, response);
    }
}
