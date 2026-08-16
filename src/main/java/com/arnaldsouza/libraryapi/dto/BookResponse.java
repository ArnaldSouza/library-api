package com.arnaldsouza.libraryapi.dto;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        Integer publishedYear,
        String genre,
        Integer availableCopies
) {
}
