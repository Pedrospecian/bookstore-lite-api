package com.bookstore.controller;

import com.bookstore.AbstractIntegrationTest;
import com.bookstore.entity.BookCategory;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa a jornada completa do cliente: adicionar livros ao carrinho, fechar o pedido
 * (checkout, com baixa real de estoque) e cancelar o pedido (com restauração do estoque).
 * Roda contra um Postgres real via Testcontainers, sem usar mocks.
 */
class OrderFlowIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String customerToken;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        User customer = userRepository.save(User.builder()
                .name("Cliente Fluxo")
                .email("fluxo-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Senha123"))
                .role(Role.CUSTOMER)
                .build());
        customerToken = jwtService.generateAccessToken(customer.getId().toString(), customer.getRole().name());

        var book = bookRepository.save(com.bookstore.entity.Book.builder()
                .title("Livro do Fluxo de Pedido")
                .author("Autor Y")
                .category(BookCategory.FICTION)
                .price(new BigDecimal("25.00"))
                .stockQuantity(10)
                .active(true)
                .build());
        bookId = book.getId();
    }

    private String auth(String token) {
        return "Bearer " + token;
    }

    @Test
    void fluxoCompletoDeCompraECancelamento() throws Exception {
        // 1. Carrinho começa vazio
        mockMvc.perform(get("/cart").header("Authorization", auth(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));

        // 2. Adiciona 3 unidades ao carrinho
        String addPayload = objectMapper.writeValueAsString(new AddPayload(bookId, 3));
        mockMvc.perform(post("/cart/items").header("Authorization", auth(customerToken))
                        .contentType("application/json").content(addPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.total").value(75.00));

        // 3. Estoque ainda não foi alterado, pois ele só é debitado no checkout
        mockMvc.perform(get("/books/" + bookId))
                .andExpect(jsonPath("$.stockQuantity").value(10));

        // 4. Checkout
        String checkoutPayload = objectMapper.writeValueAsString(new CheckoutPayload(
                "Rua Teste", "100", null, "Centro", "Fortaleza", "CE", "60000-000"
        ));
        String checkoutResponse = mockMvc.perform(post("/orders").header("Authorization", auth(customerToken))
                        .contentType("application/json").content(checkoutPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(75.00))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn().getResponse().getContentAsString();

        String orderId = objectMapper.readTree(checkoutResponse).get("id").asText();

        // 5. Estoque foi debitado (10 - 3 = 7)
        mockMvc.perform(get("/books/" + bookId))
                .andExpect(jsonPath("$.stockQuantity").value(7));

        // 6. Carrinho foi esvaziado após o checkout
        mockMvc.perform(get("/cart").header("Authorization", auth(customerToken)))
                .andExpect(jsonPath("$.items.length()").value(0));

        // 7. Pedido aparece na listagem do cliente
        mockMvc.perform(get("/orders").header("Authorization", auth(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(orderId));

        // 8. Cancela o pedido
        mockMvc.perform(post("/orders/" + orderId + "/cancel").header("Authorization", auth(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        // 9. Estoque foi restaurado (7 + 3 = 10)
        mockMvc.perform(get("/books/" + bookId))
                .andExpect(jsonPath("$.stockQuantity").value(10));

        // 10. Cancelar de novo deve falhar (pedido já está cancelado)
        mockMvc.perform(post("/orders/" + orderId + "/cancel").header("Authorization", auth(customerToken)))
                .andExpect(status().isConflict());
    }

    @Test
    void checkoutComCarrinhoVazioRetorna400() throws Exception {
        String checkoutPayload = objectMapper.writeValueAsString(new CheckoutPayload(
                "Rua Teste", "100", null, "Centro", "Fortaleza", "CE", "60000-000"
        ));

        mockMvc.perform(post("/orders").header("Authorization", auth(customerToken))
                        .contentType("application/json").content(checkoutPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adicionarQuantidadeMaiorQueEstoqueRetorna400() throws Exception {
        String addPayload = objectMapper.writeValueAsString(new AddPayload(bookId, 999));

        mockMvc.perform(post("/cart/items").header("Authorization", auth(customerToken))
                        .contentType("application/json").content(addPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acessarPedidoDeOutroUsuarioRetorna404() throws Exception {
        String addPayload = objectMapper.writeValueAsString(new AddPayload(bookId, 1));
        mockMvc.perform(post("/cart/items").header("Authorization", auth(customerToken))
                        .contentType("application/json").content(addPayload));

        String checkoutPayload = objectMapper.writeValueAsString(new CheckoutPayload(
                "Rua Teste", "100", null, "Centro", "Fortaleza", "CE", "60000-000"
        ));
        String checkoutResponse = mockMvc.perform(post("/orders").header("Authorization", auth(customerToken))
                        .contentType("application/json").content(checkoutPayload))
                .andReturn().getResponse().getContentAsString();
        String orderId = objectMapper.readTree(checkoutResponse).get("id").asText();

        User outroCliente = userRepository.save(User.builder()
                .name("Outro Cliente")
                .email("outro-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Senha123"))
                .role(Role.CUSTOMER)
                .build());
        String outroToken = jwtService.generateAccessToken(outroCliente.getId().toString(), outroCliente.getRole().name());

        mockMvc.perform(get("/orders/" + orderId).header("Authorization", auth(outroToken)))
                .andExpect(status().isNotFound());
    }

    private record AddPayload(UUID bookId, Integer quantity) {
    }

    private record CheckoutPayload(String street, String number, String complement, String neighborhood, String city, String state, String zipCode) {
    }
}
