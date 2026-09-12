package com.arnaldsouza.libraryapi.services;

import com.arnaldsouza.libraryapi.dto.BookRequest;
import com.arnaldsouza.libraryapi.dto.BookResponse;
import com.arnaldsouza.libraryapi.entity.Book;
import com.arnaldsouza.libraryapi.exception.BookNotFoundException;
import com.arnaldsouza.libraryapi.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book existingBook;

    @BeforeEach
    void setUp() {
        existingBook = new Book();
        existingBook.setId(1L);
        existingBook.setTitle("Clean Code");
        existingBook.setAuthor("Robert C. Martin");
        existingBook.setIsbn("9780132350884");
        existingBook.setPublishedYear(2008);
        existingBook.setGenre("Software");
        existingBook.setAvailableCopies(5);
    }

    @Test
    @DisplayName("create should persist the book and return its response")
    void createShouldPersistBook() {
        BookRequest request = new BookRequest(
                "Refactoring", "Martin Fowler", "9780134757599",
                2018, "Software", 4);

        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        BookResponse response = bookService.create(request);

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.title()).isEqualTo("Refactoring");
        assertThat(response.availableCopies()).isEqualTo(4);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getIsbn()).isEqualTo("9780134757599");
    }

    @Test
    @DisplayName("findById should return the book when it exists")
    void findByIdShouldReturnBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));

        BookResponse response = bookService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("findById should throw when the book does not exist")
    void findByIdShouldThrowWhenMissing() {
        when(bookRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(42L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    @DisplayName("update should overwrite fields of an existing book")
    void updateShouldOverwriteFields() {
        BookRequest request = new BookRequest(
                "Clean Code", "Robert C. Martin", "9780132350884",
                2008, "Software Engineering", 10);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        BookResponse response = bookService.update(1L, request);

        assertThat(response.genre()).isEqualTo("Software Engineering");
        assertThat(response.availableCopies()).isEqualTo(10);
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("update should throw when the book does not exist")
    void updateShouldThrowWhenMissing() {
        BookRequest request = new BookRequest(
                "Any", "Any", "123", 2020, "Any", 1);

        when(bookRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(42L, request))
                .isInstanceOf(BookNotFoundException.class);

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("delete should remove the book when it exists")
    void deleteShouldRemoveBook() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.delete(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete should throw and not call the repository when missing")
    void deleteShouldThrowWhenMissing() {
        when(bookRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.delete(42L))
                .isInstanceOf(BookNotFoundException.class);

        verify(bookRepository, never()).deleteById(any());
    }
}
