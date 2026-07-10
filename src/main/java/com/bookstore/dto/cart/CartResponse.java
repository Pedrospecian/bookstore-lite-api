package com.bookstore.dto.cart;

import com.bookstore.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        List<Item> items,
        BigDecimal total
) {
    public record Item(
            UUID id,
            UUID bookId,
            String title,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal subtotal,
            Integer availableStock
    ) {
        public static Item from(CartItem cartItem) {
            BigDecimal subtotal = cartItem.getBook().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            return new Item(
                    cartItem.getId(),
                    cartItem.getBook().getId(),
                    cartItem.getBook().getTitle(),
                    cartItem.getBook().getPrice(),
                    cartItem.getQuantity(),
                    subtotal,
                    cartItem.getBook().getStockQuantity()
            );
        }
    }

    public static CartResponse from(List<CartItem> cartItems) {
        List<Item> items = cartItems.stream().map(Item::from).toList();
        BigDecimal total = items.stream()
                .map(Item::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, total);
    }
}
