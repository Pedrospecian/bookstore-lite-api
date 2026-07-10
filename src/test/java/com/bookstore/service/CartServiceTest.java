package com.bookstore.service;

import com.bookstore.dto.cart.AddCartItemRequest;
import com.bookstore.entity.Book;
import com.bookstore.entity.BookCategory;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.User;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.CartItemRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private BookService bookService;

    private CartService cartService;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, bookService);
        user = User.builder().id(UUID.randomUUID()).build();
        book = Book.builder()
                .id(UUID.randomUUID())
                .title("Dom Casmurro")
                .price(new BigDecimal("34.90"))
                .stockQuantity(10)
                .build();
    }

    @Test
    void addItemCriaNovoItemQuandoLivroAindaNaoEstaNoCarrinho() {
        when(bookService.findById(book.getId())).thenReturn(book);
        when(cartItemRepository.findByUserIdAndBookId(user.getId(), book.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId())).thenReturn(List.of());

        cartService.addItem(user, new AddCartItemRequest(book.getId(), 2));

        verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 2 && item.getBook().equals(book)));
    }

    @Test
    void addItemSomaQuantidadeQuandoLivroJaEstaNoCarrinho() {
        CartItem existing = CartItem.builder().id(UUID.randomUUID()).user(user).book(book).quantity(3).build();

        when(bookService.findById(book.getId())).thenReturn(book);
        when(cartItemRepository.findByUserIdAndBookId(user.getId(), book.getId())).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId())).thenReturn(List.of());

        cartService.addItem(user, new AddCartItemRequest(book.getId(), 2));

        verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 5));
    }

    @Test
    void addItemLancaErroQuandoQuantidadeExcedeEstoque() {
        when(bookService.findById(book.getId())).thenReturn(book);

        assertThatThrownBy(() -> cartService.addItem(user, new AddCartItemRequest(book.getId(), 999)))
                .isInstanceOf(InsufficientStockException.class);

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void updateItemQuantityLancaErroQuandoItemNaoPertenceAoUsuario() {
        UUID itemId = UUID.randomUUID();
        when(cartItemRepository.findByIdAndUserId(itemId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(user, itemId, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItemDeletaItemDoCarrinho() {
        CartItem item = CartItem.builder().id(UUID.randomUUID()).user(user).book(book).quantity(1).build();
        when(cartItemRepository.findByIdAndUserId(item.getId(), user.getId())).thenReturn(Optional.of(item));
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId())).thenReturn(List.of());

        cartService.removeItem(user, item.getId());

        verify(cartItemRepository).delete(item);
    }
}
