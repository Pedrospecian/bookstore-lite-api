package com.bookstore.service;

import com.bookstore.dto.book.BookRequest;
import com.bookstore.dto.book.StockMovementRequest;
import com.bookstore.entity.Book;
import com.bookstore.entity.BookCategory;
import com.bookstore.entity.StockMovement;
import com.bookstore.entity.StockMovementType;
import com.bookstore.entity.User;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.BookSpecifications;
import com.bookstore.repository.StockMovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final StockMovementRepository stockMovementRepository;

    public BookService(BookRepository bookRepository, StockMovementRepository stockMovementRepository) {
        this.bookRepository = bookRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public Page<Book> search(String query, BookCategory category, Boolean active, Pageable pageable) {
        var spec = BookSpecifications.combine(
                BookSpecifications.titleOrAuthorContains(query),
                BookSpecifications.hasCategory(category),
                BookSpecifications.isActive(active)
        );
        return bookRepository.findAll(spec, pageable);
    }

    public Book findById(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livro não encontrado"));
    }

    @Transactional
    public Book create(BookRequest request) {
        Book book = Book.builder()
                .title(request.title())
                .author(request.author())
                .publisher(request.publisher())
                .category(request.category())
                .isbn(request.isbn())
                .price(request.price())
                .coverUrl(request.coverUrl())
                .synopsis(request.synopsis())
                .stockQuantity(0)
                .active(true)
                .build();

        return bookRepository.save(book);
    }

    @Transactional
    public Book update(UUID id, BookRequest request) {
        Book book = findById(id);

        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setPublisher(request.publisher());
        book.setCategory(request.category());
        book.setIsbn(request.isbn());
        book.setPrice(request.price());
        book.setCoverUrl(request.coverUrl());
        book.setSynopsis(request.synopsis());

        return bookRepository.save(book);
    }

    @Transactional
    public void deactivate(UUID id) {
        Book book = findById(id);
        book.setActive(false);
        bookRepository.save(book);
    }

    @Transactional
    public Book adjustStock(UUID id, StockMovementRequest request, User responsible) {
        Book book = findById(id);

        int delta = request.type() == StockMovementType.IN ? request.quantity() : -request.quantity();
        int newQuantity = book.getStockQuantity() + delta;

        if (newQuantity < 0) {
            throw new InsufficientStockException("Estoque insuficiente para essa movimentação de saída");
        }

        book.setStockQuantity(newQuantity);
        bookRepository.save(book);

        stockMovementRepository.save(StockMovement.builder()
                .book(book)
                .type(request.type())
                .quantity(request.quantity())
                .note(request.note())
                .createdBy(responsible)
                .build());

        return book;
    }
}
