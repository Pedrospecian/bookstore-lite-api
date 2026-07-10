package com.bookstore.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull(message = "Livro é obrigatório")
        UUID bookId,

        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero")
        Integer quantity
) {
}
