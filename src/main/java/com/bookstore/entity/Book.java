package com.bookstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(length = 150)
    private String publisher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookCategory category;

    @Column(length = 20)
    private String isbn;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
