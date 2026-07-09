package com.bookstore.controller;

import com.bookstore.AbstractIntegrationTest;
import com.bookstore.entity.BookCategory;
import com.bookstore.entity.Role;
import com.bookstore.entity.User;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    /** Cria um usuário admin direto no banco e devolve um access token válido pra ele. */
    private String adminToken() {
        User admin = userRepository.save(User.builder()
                .name("Admin de Teste")
                .email("admin-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Senha123"))
                .role(Role.ADMIN)
                .build());

        return jwtService.generateAccessToken(admin.getId().toString(), admin.getRole().name());
    }

    private record BookPayload(
            String title, String author, String publisher, BookCategory category,
            String isbn, BigDecimal price, String coverUrl, String synopsis
    ) {
    }

    @Test
    void listaLivrosPublicamenteSemAutenticacao() throws Exception {
        // O seed do Flyway (V2__seed_books.sql) já cadastra 6 livros
        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)));
    }

    @Test
    void buscaPorTituloFiltraCorretamente() throws Exception {
        mockMvc.perform(get("/books").param("q", "Clean Code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
    }

    @Test
    void createSemTokenRetorna401() throws Exception {
        String payload = objectMapper.writeValueAsString(new BookPayload(
                "Livro Sem Auth", "Autor X", "Editora Y", BookCategory.OTHER,
                null, new BigDecimal("19.90"), null, null
        ));

        mockMvc.perform(post("/books").contentType("application/json").content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createComTokenDeClienteComumRetorna403() throws Exception {
        User customer = userRepository.save(User.builder()
                .name("Cliente Comum")
                .email("cliente-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("Senha123"))
                .role(Role.CUSTOMER)
                .build());
        String token = jwtService.generateAccessToken(customer.getId().toString(), customer.getRole().name());

        String payload = objectMapper.writeValueAsString(new BookPayload(
                "Livro Bloqueado", "Autor X", "Editora Y", BookCategory.OTHER,
                null, new BigDecimal("19.90"), null, null
        ));

        mockMvc.perform(post("/books").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminConsegueCriarEditarEDesativarLivro() throws Exception {
        String token = adminToken();

        String createPayload = objectMapper.writeValueAsString(new BookPayload(
                "Livro de Teste", "Autor Teste", "Editora Teste", BookCategory.FICTION,
                "1234567890123", new BigDecimal("29.90"), null, "Sinopse de teste"
        ));

        String createResponse = mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        String bookId = objectMapper.readTree(createResponse).get("id").asText();

        String updatePayload = objectMapper.writeValueAsString(new BookPayload(
                "Livro de Teste (Editado)", "Autor Teste", "Editora Teste", BookCategory.FICTION,
                "1234567890123", new BigDecimal("39.90"), null, "Sinopse atualizada"
        ));

        mockMvc.perform(put("/books/" + bookId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Livro de Teste (Editado)"))
                .andExpect(jsonPath("$.price").value(39.90));

        mockMvc.perform(delete("/books/" + bookId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Livro desativado não aparece mais na busca pública (active=true é o filtro padrão)
        mockMvc.perform(get("/books").param("q", "Livro de Teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void adminConsegueMovimentarEstoque() throws Exception {
        String token = adminToken();

        String createPayload = objectMapper.writeValueAsString(new BookPayload(
                "Livro para Estoque", "Autor Teste", "Editora Teste", BookCategory.EDUCATION,
                null, new BigDecimal("50.00"), null, null
        ));
        String createResponse = mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(createPayload))
                .andReturn().getResponse().getContentAsString();
        String bookId = objectMapper.readTree(createResponse).get("id").asText();

        String stockInPayload = """
                {"type": "IN", "quantity": 20, "note": "Reposição inicial"}
                """;

        mockMvc.perform(post("/books/" + bookId + "/stock")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(stockInPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(20));

        String stockOutPayload = """
                {"type": "OUT", "quantity": 25, "note": "Saída maior que o estoque"}
                """;

        mockMvc.perform(post("/books/" + bookId + "/stock")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(stockOutPayload))
                .andExpect(status().isBadRequest());
    }
}
