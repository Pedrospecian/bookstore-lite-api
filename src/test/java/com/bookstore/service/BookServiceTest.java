package com.bookstore.service;

import com.bookstore.dto.book.BookRequest;
import com.bookstore.dto.book.StockMovementRequest;
import com.bookstore.entity.*;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, stockMovementRepository);
    }

    private Book sampleBook(int stock) {
        return Book.builder()
                .id(UUID.randomUUID())
                .title("Dom Casmurro")
                .author("Machado de Assis")
                .category(BookCategory.FICTION)
                .price(new BigDecimal("34.90"))
                .stockQuantity(stock)
                .active(true)
                .build();
    }

    @Test
    void findByIdLancaExcecaoQuandoLivroNaoExiste() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Livro não encontrado");
    }

    @Test
    void createSalvaLivroComEstoqueZeradoEAtivo() {
        var request = new BookRequest(
                "Clean Code", "Robert C. Martin", "Alta Books", BookCategory.MANUAL,
                "9788576082675", new BigDecimal("89.90"), null, null
        );
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book created = bookService.create(request);

        assertThat(created.getStockQuantity()).isZero();
        assertThat(created.getActive()).isTrue();
        assertThat(created.getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void adjustStockAumentaEstoqueEmMovimentacaoDeEntrada() {
        Book book = sampleBook(10);
        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new StockMovementRequest(StockMovementType.IN, 5, "Reposição");
        Book updated = bookService.adjustStock(book.getId(), request, admin);

        assertThat(updated.getStockQuantity()).isEqualTo(15);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(StockMovementType.IN);
        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(admin);
    }

    @Test
    void adjustStockDiminuiEstoqueEmMovimentacaoDeSaida() {
        Book book = sampleBook(10);
        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new StockMovementRequest(StockMovementType.OUT, 4, "Ajuste manual");
        Book updated = bookService.adjustStock(book.getId(), request, admin);

        assertThat(updated.getStockQuantity()).isEqualTo(6);
    }

    @Test
    void adjustStockLancaErroQuandoSaidaDeixariaEstoqueNegativo() {
        Book book = sampleBook(3);
        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();

        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        var request = new StockMovementRequest(StockMovementType.OUT, 10, null);

        assertThatThrownBy(() -> bookService.adjustStock(book.getId(), request, admin))
                .isInstanceOf(InsufficientStockException.class);

        verify(bookRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void deactivateMarcaLivroComoInativoSemExcluirDoBanco() {
        Book book = sampleBook(10);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        bookService.deactivate(book.getId());

        assertThat(book.getActive()).isFalse();
        verify(bookRepository, never()).delete(any(Book.class));
        verify(bookRepository, never()).deleteById(any());
    }
}
