package com.bookstore.service;

import com.bookstore.dto.book.StockMovementRequest;
import com.bookstore.dto.order.CheckoutRequest;
import com.bookstore.entity.*;
import com.bookstore.exception.EmptyCartException;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.InvalidOrderStateException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private BookService bookService;

    private OrderService orderService;

    private User user;
    private CheckoutRequest checkoutRequest;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, cartItemRepository, bookService);
        user = User.builder().id(UUID.randomUUID()).build();
        checkoutRequest = new CheckoutRequest(
                "Rua das Flores", "123", null, "Centro", "Fortaleza", "ce", "60000-000"
        );
    }

    private CartItem cartItemFor(BigDecimal price, int quantity) {
        Book book = Book.builder().id(UUID.randomUUID()).title("Livro Teste").price(price).stockQuantity(50).build();
        return CartItem.builder().id(UUID.randomUUID()).user(user).book(book).quantity(quantity).build();
    }

    @Test
    void checkoutLancaErroQuandoCarrinhoEstaVazio() {
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(user, checkoutRequest))
                .isInstanceOf(EmptyCartException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkoutCriaPedidoComTotalCorretoEEsvaziaCarrinho() {
        CartItem item1 = cartItemFor(new BigDecimal("30.00"), 2); // 60.00
        CartItem item2 = cartItemFor(new BigDecimal("15.50"), 3); // 46.50
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId())).thenReturn(List.of(item1, item2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.checkout(user, checkoutRequest);

        assertThat(order.getTotalAmount()).isEqualByComparingTo("106.50");
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getAddress().getState()).isEqualTo("CE"); // normalizado pra maiúsculo

        // Cada item do carrinho deve gerar uma baixa de estoque
        verify(bookService).adjustStock(eq(item1.getBook().getId()),
                argThat(r -> r.type() == StockMovementType.OUT && r.quantity() == 2), eq(user));
        verify(bookService).adjustStock(eq(item2.getBook().getId()),
                argThat(r -> r.type() == StockMovementType.OUT && r.quantity() == 3), eq(user));

        verify(cartItemRepository).deleteByUserId(user.getId());
    }

    @Test
    void checkoutPropagaErroDeEstoqueInsuficienteSemSalvarPedido() {
        CartItem item = cartItemFor(new BigDecimal("30.00"), 100);
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId())).thenReturn(List.of(item));
        when(bookService.adjustStock(any(), any(StockMovementRequest.class), eq(user)))
                .thenThrow(new InsufficientStockException("sem estoque"));

        assertThatThrownBy(() -> orderService.checkout(user, checkoutRequest))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteByUserId(any());
    }

    @Test
    void findByIdForUserLancaNotFoundQuandoPedidoNaoEhDoUsuario() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndUserId(orderId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findByIdForUser(user, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelRestauraEstoqueDeCadaItemEMudaStatus() {
        Order order = Order.builder().id(UUID.randomUUID()).user(user).status(OrderStatus.PLACED).build();
        Book book = Book.builder().id(UUID.randomUUID()).build();
        OrderItem orderItem = OrderItem.builder().book(book).quantity(4).unitPrice(new BigDecimal("10.00")).bookTitle("X").build();
        order.addItem(orderItem);

        when(orderRepository.findByIdAndUserId(order.getId(), user.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order canceled = orderService.cancel(user, order.getId());

        assertThat(canceled.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(bookService).adjustStock(eq(book.getId()),
                argThat(r -> r.type() == StockMovementType.IN && r.quantity() == 4), eq(user));
    }

    @Test
    void cancelLancaErroQuandoPedidoJaEstaCancelado() {
        Order order = Order.builder().id(UUID.randomUUID()).user(user).status(OrderStatus.CANCELED).build();
        when(orderRepository.findByIdAndUserId(order.getId(), user.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(user, order.getId()))
                .isInstanceOf(InvalidOrderStateException.class);

        verify(bookService, never()).adjustStock(any(), any(), any());
    }
}
