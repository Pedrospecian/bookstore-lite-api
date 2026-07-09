package com.bookstore.repository;

import com.bookstore.entity.Book;
import com.bookstore.entity.BookCategory;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> titleOrAuthorContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("author")), pattern)
        );
    }

    public static Specification<Book> hasCategory(BookCategory category) {
        if (category == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Book> isActive(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Book> combine(Specification<Book>... specs) {
        Specification<Book> result = Specification.where(null);
        for (Specification<Book> spec : specs) {
            if (spec != null) {
                result = result.and(spec);
            }
        }
        return result;
    }
}
