package com.bookstore.service;

import com.bookstore.dto.cart.AddCartItemRequest;
import com.bookstore.entity.Book;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.User;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final BookService bookService;

    public CartService(CartItemRepository cartItemRepository, BookService bookService) {
        this.cartItemRepository = cartItemRepository;
        this.bookService = bookService;
    }

    public List<CartItem> getCart(User user) {
        return cartItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
    }

    @Transactional
    public List<CartItem> addItem(User user, AddCartItemRequest request) {
        Book book = bookService.findById(request.bookId());
        validateStock(book, request.quantity());

        CartItem item = cartItemRepository.findByUserIdAndBookId(user.getId(), request.bookId())
                .orElseGet(() -> CartItem.builder().user(user).book(book).quantity(0).build());

        int newQuantity = item.getQuantity() + request.quantity();
        validateStock(book, newQuantity);
        item.setQuantity(newQuantity);

        cartItemRepository.save(item);
        return getCart(user);
    }

    @Transactional
    public List<CartItem> updateItemQuantity(User user, UUID cartItemId, int quantity) {
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado no carrinho"));

        validateStock(item.getBook(), quantity);
        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return getCart(user);
    }

    @Transactional
    public List<CartItem> removeItem(User user, UUID cartItemId) {
        CartItem item = cartItemRepository.findByIdAndUserId(cartItemId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado no carrinho"));

        cartItemRepository.delete(item);
        return getCart(user);
    }

    @Transactional
    public void clearCart(User user) {
        cartItemRepository.deleteByUserId(user.getId());
    }

    private void validateStock(Book book, int desiredQuantity) {
        if (desiredQuantity > book.getStockQuantity()) {
            throw new InsufficientStockException(
                    "Estoque insuficiente para \"" + book.getTitle() + "\" (disponível: " + book.getStockQuantity() + ")"
            );
        }
    }
}
