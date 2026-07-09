package com.bookstore.dto.book;

import com.bookstore.entity.Book;
import com.bookstore.entity.BookCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String title,
        String author,
        String publisher,
        BookCategory category,
        String isbn,
        BigDecimal price,
        Integer stockQuantity,
        String coverUrl,
        String synopsis,
        Boolean active
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getCategory(),
                book.getIsbn(),
                book.getPrice(),
                book.getStockQuantity(),
                book.getCoverUrl(),
                book.getSynopsis(),
                book.getActive()
        );
    }
}
