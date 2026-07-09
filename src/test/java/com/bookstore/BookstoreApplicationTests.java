package com.bookstore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BookstoreApplicationTests {

    @Test
    void contextLoads() {
        // Se o contexto do Spring subir sem erros (incluindo Security, JPA e os beans
        // de JWT), esse teste passa. É uma rede de segurança básica contra erros de
        // configuração — os testes de unidade/integração de verdade vêm na próxima fase.
    }
}
