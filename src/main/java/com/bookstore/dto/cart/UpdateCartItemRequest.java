package com.bookstore.dto.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero")
        Integer quantity
) {
}
