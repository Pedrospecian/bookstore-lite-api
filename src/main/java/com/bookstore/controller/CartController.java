package com.bookstore.controller;

import com.bookstore.dto.cart.AddCartItemRequest;
import com.bookstore.dto.cart.CartResponse;
import com.bookstore.dto.cart.UpdateCartItemRequest;
import com.bookstore.entity.User;
import com.bookstore.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(CartResponse.from(cartService.getCart(user)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ResponseEntity.ok(CartResponse.from(cartService.addItem(user, request)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal User user,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ResponseEntity.ok(CartResponse.from(cartService.updateItemQuantity(user, itemId, request.quantity())));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal User user, @PathVariable UUID itemId) {
        return ResponseEntity.ok(CartResponse.from(cartService.removeItem(user, itemId)));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }
}
