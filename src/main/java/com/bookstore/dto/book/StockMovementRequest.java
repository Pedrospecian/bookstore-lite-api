package com.bookstore.dto.book;

import com.bookstore.entity.StockMovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockMovementRequest(
        @NotNull(message = "Tipo de movimentação é obrigatório")
        StockMovementType type,

        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero")
        Integer quantity,

        String note
) {
}
