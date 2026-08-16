package com.arnaldsouza.libraryapi.service;

import com.arnaldsouza.libraryapi.dto.BookRequest;
import com.arnaldsouza.libraryapi.dto.BookResponse;
import com.arnaldsouza.libraryapi.entity.Book;
import com.arnaldsouza.libraryapi.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public BookResponse create(BookRequest request) {
        Book book = new Book();
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setPublishedYear(request.publishedYear());
        book.setGenre(request.genre());
        book.setAvailableCopies(request.availableCopies());

        Book savedBook = bookRepository.save(book);
        return toResponse(savedBook);
    }

    public List<BookResponse> findAll() {
        return bookRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BookResponse findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        return toResponse(book);
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