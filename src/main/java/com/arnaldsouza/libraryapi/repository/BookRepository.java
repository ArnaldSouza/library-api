package com.arnaldsouza.libraryapi.repository;

import com.arnaldsouza.libraryapi.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}