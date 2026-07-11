package com.bookstore.service;

import com.bookstore.dto.book.StockMovementRequest;
import com.bookstore.dto.order.CheckoutRequest;
import com.bookstore.entity.*;
import com.bookstore.exception.EmptyCartException;
import com.bookstore.exception.InvalidOrderStateException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final BookService bookService;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository, BookService bookService) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookService = bookService;
    }

    /**
     * Converte o carrinho do usuário em um pedido: valida estoque de cada item,
     * dá baixa no estoque (gerando o histórico de movimentação via BookService),
     * grava um snapshot de título/preço em cada OrderItem e esvazia o carrinho.
     * Tudo fica dentro de uma única transação, e se qualquer item falhar por falta
     * de estoque, então nada será persistido.
     */
    @Transactional
    public Order checkout(User user, CheckoutRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId());

        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Carrinho está vazio");
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PLACED)
                .address(new Address(
                        request.street(), request.number(), request.complement(),
                        request.neighborhood(), request.city(), request.state().toUpperCase(), request.zipCode()
                ))
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();

            // Dá baixa no estoque reaproveitando a mesma regra usada pelo admin
            // (inclusive o histórico de StockMovement). Se não houver estoque
            // suficiente, adjustStock lança InsufficientStockException e a
            // transação inteira do checkout é revertida.
            bookService.adjustStock(
                    book.getId(),
                    new StockMovementRequest(StockMovementType.OUT, cartItem.getQuantity(), "Venda - pedido"),
                    user
            );

            OrderItem orderItem = OrderItem.builder()
                    .book(book)
                    .bookTitle(book.getTitle())
                    .unitPrice(book.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build();
            order.addItem(orderItem);

            total = total.add(book.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setTotalAmount(total);
        orderRepository.save(order);

        cartItemRepository.deleteByUserId(user.getId());

        return order;
    }

    public Page<Order> listOrders(User user, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    public Order findByIdForUser(User user, UUID orderId) {
        // Usa NotFound (não Forbidden) quando o pedido não é do usuário.
        // Isso evita confirmar pra quem não tem acesso que aquele ID de pedido existe.
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));
    }

    @Transactional
    public Order cancel(User user, UUID orderId) {
        Order order = findByIdForUser(user, orderId);

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new InvalidOrderStateException("Só é possível cancelar pedidos com status PLACED");
        }

        for (OrderItem item : order.getItems()) {
            bookService.adjustStock(
                    item.getBook().getId(),
                    new StockMovementRequest(StockMovementType.IN, item.getQuantity(), "Cancelamento de pedido"),
                    user
            );
        }

        order.setStatus(OrderStatus.CANCELED);
        return orderRepository.save(order);
    }
}
