package com.bookstore.controller;

import com.bookstore.dto.book.BookRequest;
import com.bookstore.dto.book.BookResponse;
import com.bookstore.dto.book.StockMovementRequest;
import com.bookstore.entity.Book;
import com.bookstore.entity.BookCategory;
import com.bookstore.entity.User;
import com.bookstore.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<Page<BookResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BookCategory category,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            Pageable pageable
    ) {
        Page<BookResponse> books = bookService.search(q, category, active, pageable).map(BookResponse::from);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(BookResponse.from(bookService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        Book book = bookService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.from(book));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> update(@PathVariable UUID id, @Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(BookResponse.from(bookService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        bookService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stock")
    public ResponseEntity<BookResponse> adjustStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal User admin
    ) {
        Book book = bookService.adjustStock(id, request, admin);
        return ResponseEntity.ok(BookResponse.from(book));
    }
}
