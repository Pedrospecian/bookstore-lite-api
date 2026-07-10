package com.bookstore.dto.order;

import com.bookstore.entity.Address;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        List<Item> items,
        AddressResponse address,
        BigDecimal totalAmount,
        Instant createdAt
) {
    public record Item(UUID bookId, String title, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal) {
        public static Item from(OrderItem item) {
            return new Item(item.getBook().getId(), item.getBookTitle(), item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
        }
    }

    public record AddressResponse(String street, String number, String complement, String neighborhood, String city, String state, String zipCode) {
        public static AddressResponse from(Address address) {
            return new AddressResponse(
                    address.getStreet(), address.getNumber(), address.getComplement(),
                    address.getNeighborhood(), address.getCity(), address.getState(), address.getZipCode()
            );
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItems().stream().map(Item::from).toList(),
                AddressResponse.from(order.getAddress()),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
