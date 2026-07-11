# Bookstore API

Versão "lite" de um sistema de e-commerce de livros originalmente feito com Java Servlet + JDBC puro + JSP. Este projeto reconstrói o mesmo domínio com uma stack moderna: **Spring Boot 3, Java 21, Spring Security (JWT), Spring Data JPA e PostgreSQL**, migrado com Flyway.

Este é o backend de um projeto full-stack. O frontend (Next.js) fica em um repositório separado.

## Por que essa migração

O projeto original era um sistema completo de e-commerce com múltiplos módulos administrativos (grupos de precificação, cupons, fornecedores, trocas, relatórios). Esta versão reconstrói o **núcleo do domínio** (catálogo, carrinho, checkout e administração básica de estoque) com práticas atuais de arquitetura backend: autenticação stateless com JWT, migrations versionadas, validação declarativa, tratamento de erros centralizado e testes automatizados.

### Escopo desta versão

✅ Autenticação (registro/login/refresh) com JWT
✅ Catálogo de livros com busca e filtro por categoria
✅ CRUD de livros (admin)
✅ Controle de estoque com histórico de movimentações (entrada/saída)
✅ Carrinho de compras (persistido por usuário)
✅ Checkout e pedidos, com baixa/restauração de estoque e cancelamento

❌ **Fora de escopo, propositalmente:** grupos de precificação, cupons de desconto/troca, cadastro de fornecedores, gráficos de vendas, múltiplos endereços/cartões por cliente. O sistema original tinha esses módulos; esta versão foca no fluxo principal de compra.

**Nota de modelagem:** o endereço de entrega é preenchido a cada checkout (não existe um cadastro de endereços do cliente), e o pagamento é simulado. O pedido nasce direto como `PLACED`, sem integração real com gateway de pagamento.

## Rodando localmente

### Pré-requisitos
- Java 21
- Maven (ou use o `./mvnw` se preferir adicionar o wrapper)
- Docker (para o Postgres local) ou um Postgres já rodando

### Passos

```bash
# 1. Sobe o Postgres local
docker compose up -d

# 2. Configura as variáveis de ambiente (ou exporte no shell)
export DATABASE_URL=jdbc:postgresql://localhost:5432/bookstore-lite
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export JWT_SECRET=troque-por-um-segredo-de-pelo-menos-32-caracteres

# 3. Roda a aplicação (as migrations do Flyway rodam automaticamente)
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. Documentação interativa (Swagger) em `http://localhost:8080/docs`.

### Criando o primeiro usuário admin

Não há seed de usuário admin (senha em seed exigiria um hash bcrypt fixo no controle de versão, o que não é uma boa prática). Em vez disso:

1. Registre um usuário normalmente via `POST /auth/register`
2. Promova-o a admin diretamente no banco:
   ```sql
   UPDATE users SET role = 'ADMIN' WHERE email = 'seu-email@exemplo.com';
   ```

### Testes

```bash
mvn test
```

O projeto tem duas camadas de teste:
- **Unitários** (`*Test.java`): `AuthServiceTest`, `BookServiceTest`, `JwtServiceTest`. Usam Mockito, não tocam banco nem precisam de Docker.
- **Integração** (`*IT.java`): `AuthControllerIT`, `BookControllerIT`, `OrderFlowIT`. Sobem um Postgres real via **Testcontainers** e batem nas rotas HTTP de verdade com MockMvc, incluindo as regras de autorização por role (`ROLE_ADMIN` vs `ROLE_CUSTOMER`) e a jornada completa carrinho → checkout → cancelamento (com baixa e restauração de estoque de verdade). **Exigem Docker rodando na máquina.**

## Variáveis de ambiente

| Variável | Descrição | Padrão (dev) |
|---|---|---|
| `DATABASE_URL` | URL JDBC do Postgres | `jdbc:postgresql://localhost:5432/bookstore-lite` |
| `DATABASE_USERNAME` | Usuário do banco | `postgres` |
| `DATABASE_PASSWORD` | Senha do banco | `postgres` |
| `JWT_SECRET` | Segredo usado para assinar os tokens (min. 32 caracteres) | — |
| `JWT_ACCESS_EXPIRATION_MS` | Expiração do access token (ms) | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Expiração do refresh token (ms) | `604800000` (7 dias) |
| `CORS_ALLOWED_ORIGINS` | Origem(ns) permitida(s) para CORS | `http://localhost:3000` |
| `PORT` | Porta HTTP | `8080` |

## Stack

- Java 21 + Spring Boot 3.3
- Spring Web, Spring Data JPA, Spring Security
- PostgreSQL + Flyway
- JWT (jjwt)
- springdoc-openapi (Swagger UI)
- JUnit 5, H2 (testes)
