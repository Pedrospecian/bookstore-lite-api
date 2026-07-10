package com.bookstore.repository;

import com.bookstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    List<CartItem> findByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<CartItem> findByUserIdAndBookId(UUID userId, UUID bookId);
    Optional<CartItem> findByIdAndUserId(UUID id, UUID userId);
    void deleteByUserId(UUID userId);
}
