package com.example.library;

import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    // --- POSITIVE TESTS (as LIBRARIAN — has write access) ---

    @Test
    @DisplayName("A1: Successfully create a valid book")
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook_Success() throws Exception {
        Book book = new Book("Clean Code", "Robert C. Martin", "9780132350884");

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Clean Code")))
                .andExpect(jsonPath("$.author", is("Robert C. Martin")))
                .andExpect(jsonPath("$.isbn", is("9780132350884")));

        assertEquals(1, bookRepository.count());
    }

    @Test
    @DisplayName("A2: Retrieve all books")
    @WithMockUser(roles = "USER")
    void testGetAllBooks_Success() throws Exception {
        bookRepository.save(new Book("Clean Code", "Robert C. Martin", "9780132350884"));
        bookRepository.save(new Book("The Pragmatic Programmer", "Andy Hunt", "9780201616224"));

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Clean Code")))
                .andExpect(jsonPath("$[1].title", is("The Pragmatic Programmer")));
    }

    @Test
    @DisplayName("A3: Retrieve book by ID")
    @WithMockUser(roles = "USER")
    void testGetBookById_Success() throws Exception {
        Book saved = bookRepository.save(new Book("Clean Code", "Robert C. Martin", "9780132350884"));

        mockMvc.perform(get("/books/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Clean Code")))
                .andExpect(jsonPath("$.author", is("Robert C. Martin")))
                .andExpect(jsonPath("$.isbn", is("9780132350884")));
    }

    @Test
    @DisplayName("A4 & A5: Update book and verify persistence")
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateBook_Success() throws Exception {
        Book saved = bookRepository.save(new Book("Clean Code", "Robert C. Martin", "9780132350884"));

        Book updated = new Book("Clean Code: Second Edition", "Uncle Bob", "9780132350884");

        mockMvc.perform(put("/books/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Clean Code: Second Edition")))
                .andExpect(jsonPath("$.author", is("Uncle Bob")))
                .andExpect(jsonPath("$.isbn", is("9780132350884")));

        Book inDb = bookRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Clean Code: Second Edition", inDb.getTitle());
        assertEquals("Uncle Bob", inDb.getAuthor());
    }

    @Test
    @DisplayName("A6 & A7: Delete book and verify it is no longer available")
    @WithMockUser(roles = "ADMIN")
    void testDeleteBook_Success() throws Exception {
        Book saved = bookRepository.save(new Book("Clean Code", "Robert C. Martin", "9780132350884"));

        mockMvc.perform(delete("/books/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertFalse(bookRepository.existsById(saved.getId()));

        mockMvc.perform(get("/books/{id}", saved.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    // --- NEGATIVE TESTS (as LIBRARIAN — to test validation, not auth) ---

    @Test
    @DisplayName("B1: Reject book with empty title")
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook_EmptyTitle() throws Exception {
        Book book = new Book("", "Robert C. Martin", "9780132350884");

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.title", notNullValue()));
    }

    @Test
    @DisplayName("B2: Reject book with missing author")
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook_MissingAuthor() throws Exception {
        Book book = new Book("Clean Code", "", "9780132350884");

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.author", notNullValue()));
    }

    @Test
    @DisplayName("B3: Reject book with invalid ISBN")
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook_InvalidIsbn() throws Exception {
        Book book = new Book("Clean Code", "Robert C. Martin", "invalid-isbn-123");

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.isbn", is("Invalid ISBN format")));
    }

    @Test
    @DisplayName("B4: Reject malformed JSON body")
    @WithMockUser(roles = "LIBRARIAN")
    void testCreateBook_MalformedJson() throws Exception {
        String malformedJson = "{ \"title\": \"Clean Code\", \"author\": ";

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
    }

    @Test
    @DisplayName("B5: Return 404 when retrieving nonexistent book")
    @WithMockUser(roles = "USER")
    void testGetNonexistentBook() throws Exception {
        mockMvc.perform(get("/books/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("B6: Return 404 when updating nonexistent book")
    @WithMockUser(roles = "LIBRARIAN")
    void testUpdateNonexistentBook() throws Exception {
        Book updated = new Book("Clean Code", "Robert C. Martin", "9780132350884");

        mockMvc.perform(put("/books/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("B7: Return 404 when deleting nonexistent book")
    @WithMockUser(roles = "ADMIN")
    void testDeleteNonexistentBook() throws Exception {
        mockMvc.perform(delete("/books/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("B8: Reject duplicate ISBN on creation and update")
    @WithMockUser(roles = "LIBRARIAN")
    void testDuplicateIsbn() throws Exception {
        bookRepository.save(new Book("Clean Code", "Robert C. Martin", "9780132350884"));
        Book b2 = bookRepository.save(new Book("The Pragmatic Programmer", "Andy Hunt", "9780201616224"));

        // Duplicate ISBN on POST
        Book duplicate = new Book("Clean Architecture", "Robert C. Martin", "9780132350884");
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));

        // Duplicate ISBN on PUT (changing b2's ISBN to b1's ISBN)
        Book updateToExistingIsbn = new Book("The Pragmatic Programmer", "Andy Hunt", "9780132350884");
        mockMvc.perform(put("/books/{id}", b2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateToExistingIsbn)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("B9: Reject invalid ID data type")
    @WithMockUser(roles = "USER")
    void testInvalidIdType() throws Exception {
        mockMvc.perform(get("/books/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("B10: Invalid requests do not corrupt existing data")
    @WithMockUser(roles = "LIBRARIAN")
    void testInvalidRequestsDoNotCorruptData() throws Exception {
        Book original = bookRepository.save(new Book("Original Title", "Original Author", "9780132350884"));

        // Attempt invalid update (empty author)
        Book invalidUpdate = new Book("New Title", "", "9780132350884");
        mockMvc.perform(put("/books/{id}", original.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdate)))
                .andExpect(status().isBadRequest());

        // Verify original data remains untouched
        Book afterAttempt = bookRepository.findById(original.getId()).orElseThrow();
        assertEquals("Original Title", afterAttempt.getTitle());
        assertEquals("Original Author", afterAttempt.getAuthor());
    }
}
