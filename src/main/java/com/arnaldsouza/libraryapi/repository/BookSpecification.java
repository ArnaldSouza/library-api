package com.arnaldsouza.libraryapi.repository;

import com.arnaldsouza.libraryapi.entity.Book;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> withFilters(String author, String genre) {
        return (root, query, criteriaBuilder) -> {
            Specification<Book> spec = Specification.unrestricted();

            if (author != null && !author.isBlank()) {
                spec = spec.and((r, q, cb) ->
                        cb.like(cb.lower(r.get("author")), "%" + author.toLowerCase() + "%"));
            }

            if (genre != null && !genre.isBlank()) {
                spec = spec.and((r, q, cb) ->
                        cb.equal(cb.lower(r.get("genre")), genre.toLowerCase()));
            }

            return spec.toPredicate(root, query, criteriaBuilder);
        };
    }
}