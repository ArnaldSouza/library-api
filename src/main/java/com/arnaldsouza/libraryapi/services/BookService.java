package com.arnaldsouza.libraryapi.services;

import com.arnaldsouza.libraryapi.dto.BookRequest;
import com.arnaldsouza.libraryapi.dto.BookResponse;
import com.arnaldsouza.libraryapi.entity.Book;
import com.arnaldsouza.libraryapi.exception.BookNotFoundException;
import com.arnaldsouza.libraryapi.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.arnaldsouza.libraryapi.repository.BookSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public BookResponse create(BookRequest request) {
        Book book = new Book();
        applyRequest(book, request);
        Book savedBook = bookRepository.save(book);
        return toResponse(savedBook);
    }

    public Page<BookResponse> findAll(String author, String genre, Pageable pageable) {
        Specification<Book> spec = BookSpecification.withFilters(author, genre);
        return bookRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    public BookResponse findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        return toResponse(book);
    }

    public BookResponse update(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        applyRequest(book, request);
        Book updatedBook = bookRepository.save(book);
        return toResponse(updatedBook);
    }

    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        bookRepository.deleteById(id);
    }

    private void applyRequest(Book book, BookRequest request) {
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setPublishedYear(request.publishedYear());
        book.setGenre(request.genre());
        book.setAvailableCopies(request.availableCopies());
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublishedYear(),
                book.getGenre(),
                book.getAvailableCopies()
        );
    }
}