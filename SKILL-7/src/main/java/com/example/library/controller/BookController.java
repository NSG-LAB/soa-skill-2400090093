package com.example.library.controller;

import com.example.library.entity.Book;
import com.example.library.exception.ErrorResponse;
import com.example.library.repository.BookRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository repository;

    public BookController(BookRepository repository) {
        this.repository = repository;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<?> addBook(@Valid @RequestBody Book book, HttpServletRequest request) {

        if (repository.existsByIsbn(book.getIsbn())) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.CONFLICT.value(),
                    "Conflict",
                    "A book with ISBN '" + book.getIsbn() + "' already exists",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        book.setId(null);
        Book savedBook = repository.save(book);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Book>> getBooks() {

        return ResponseEntity.ok(repository.findAll());
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<?> getBook(@PathVariable Long id, HttpServletRequest request) {

        return repository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> {
                    ErrorResponse error = new ErrorResponse(
                            HttpStatus.NOT_FOUND.value(),
                            "Not Found",
                            "Book with ID " + id + " was not found",
                            request.getRequestURI()
                    );
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                });
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody Book book,
            HttpServletRequest request) {

        Optional<Book> optionalBook = repository.findById(id);
        if (optionalBook.isEmpty()) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    "Book with ID " + id + " was not found",
                    request.getRequestURI()
                    );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        // Check if ISBN is used by another book
        Optional<Book> existingWithIsbn = repository.findByIsbn(book.getIsbn());
        if (existingWithIsbn.isPresent() && !existingWithIsbn.get().getId().equals(id)) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.CONFLICT.value(),
                    "Conflict",
                    "A book with ISBN '" + book.getIsbn() + "' already exists",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        Book existingBook = optionalBook.get();
        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setIsbn(book.getIsbn());

        Book updatedBook = repository.save(existingBook);
        return ResponseEntity.ok(updatedBook);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id, HttpServletRequest request) {

        if (!repository.existsById(id)) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    "Not Found",
                    "Book with ID " + id + " was not found",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        repository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
