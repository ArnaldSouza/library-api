package com.arnaldsouza.libraryapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BookRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Author is required")
        String author,

        @NotBlank(message = "ISBN is required")
        String isbn,

        Integer publishedYear,

        String genre,

        @NotNull(message = "Available copies is required")
        @PositiveOrZero(message = "Available copies cannot be negative")
        Integer availableCopies
) {
}