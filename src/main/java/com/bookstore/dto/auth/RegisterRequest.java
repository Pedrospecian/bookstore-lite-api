package com.bookstore.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, message = "Nome deve ter ao menos 2 caracteres")
        String name,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "Senha deve ter ao menos 8 caracteres")
        @Pattern(regexp = ".*[A-Z].*", message = "Senha deve conter ao menos uma letra maiúscula")
        @Pattern(regexp = ".*[0-9].*", message = "Senha deve conter ao menos um número")
        String password
) {
}
