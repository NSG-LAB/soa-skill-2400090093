package com.example.library.controller;

import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository repository;

    public BookController(BookRepository repository) {
        this.repository = repository;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Book> addBook(@Valid @RequestBody Book book) {

        if (repository.existsByIsbn(book.getIsbn())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

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
    public ResponseEntity<Book> getBook(@PathVariable Long id) {

        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody Book book) {

        return repository.findById(id)
                .map(existingBook -> {

                    existingBook.setTitle(book.getTitle());
                    existingBook.setAuthor(book.getAuthor());
                    existingBook.setIsbn(book.getIsbn());

                    return ResponseEntity.ok(
                            repository.save(existingBook)
                    );
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {

        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}
