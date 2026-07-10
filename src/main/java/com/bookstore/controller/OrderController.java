package com.bookstore.controller;

import com.bookstore.dto.order.CheckoutRequest;
import com.bookstore.dto.order.OrderResponse;
import com.bookstore.entity.User;
import com.bookstore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CheckoutRequest request
    ) {
        var order = orderService.checkout(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> list(@AuthenticationPrincipal User user, Pageable pageable) {
        return ResponseEntity.ok(orderService.listOrders(user, pageable).map(OrderResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(orderService.findByIdForUser(user, id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return ResponseEntity.ok(OrderResponse.from(orderService.cancel(user, id)));
    }
}
