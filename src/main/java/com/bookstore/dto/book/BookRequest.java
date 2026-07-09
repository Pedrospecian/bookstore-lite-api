package com.bookstore.dto.book;

import com.bookstore.entity.BookCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BookRequest(
        @NotBlank(message = "Título é obrigatório")
        String title,

        @NotBlank(message = "Autor é obrigatório")
        String author,

        String publisher,

        @NotNull(message = "Categoria é obrigatória")
        BookCategory category,

        String isbn,

        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal price,

        String coverUrl,

        String synopsis
) {
}
