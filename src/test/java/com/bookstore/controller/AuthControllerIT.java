package com.bookstore.controller;

import com.bookstore.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerCriaUsuarioERetorna201ComTokens() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new RegisterPayload("Ana Silva", "ana.integracao@example.com", "Senha123")
        );

        mockMvc.perform(post("/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("ana.integracao@example.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void registerRetorna409QuandoEmailJaExiste() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new RegisterPayload("Ana Silva", "duplicado@example.com", "Senha123")
        );

        mockMvc.perform(post("/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }

    @Test
    void registerRetorna422ComSenhaFraca() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new RegisterPayload("Ana Silva", "ana.fraca@example.com", "123")
        );

        mockMvc.perform(post("/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginEFluxoCompletoDeAutenticacao() throws Exception {
        String registerPayload = objectMapper.writeValueAsString(
                new RegisterPayload("Bia Souza", "bia@example.com", "Senha123")
        );
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerPayload))
                .andExpect(status().isCreated());

        String loginPayload = objectMapper.writeValueAsString(
                new LoginPayload("bia@example.com", "Senha123")
        );

        String responseJson = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginPayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(responseJson).get("accessToken").asText();

        // Token válido dá acesso a /auth/me
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bia@example.com"));
    }

    @Test
    void loginRetorna401ComSenhaErrada() throws Exception {
        String registerPayload = objectMapper.writeValueAsString(
                new RegisterPayload("Carlos Lima", "carlos@example.com", "Senha123")
        );
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerPayload))
                .andExpect(status().isCreated());

        String loginPayload = objectMapper.writeValueAsString(
                new LoginPayload("carlos@example.com", "SenhaErrada1")
        );

        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private record RegisterPayload(String name, String email, String password) {
    }

    private record LoginPayload(String email, String password) {
    }
}
