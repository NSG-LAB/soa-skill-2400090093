package com.example.library;

import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import com.example.library.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String librarianToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        bookRepository.save(new Book("Existing Book", "Existing Author", "9780132350884"));

        adminToken = jwtService.generateToken("admin", "ADMIN");
        librarianToken = jwtService.generateToken("librarian", "LIBRARIAN");
        userToken = jwtService.generateToken("user", "USER");
    }

    // ===== ADMIN TESTS =====

    @Test
    @DisplayName("E1: ADMIN can create a book")
    void testAdminCreateBook() throws Exception {
        Book book = new Book("Admin Book", "Admin Author", "9780201616224");

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Admin Book")));
    }

    @Test
    @DisplayName("E2: ADMIN can update a book")
    void testAdminUpdateBook() throws Exception {
        Book existing = bookRepository.findAll().get(0);
        Book updated = new Book("Updated by Admin", "Admin", "9780132350884");

        mockMvc.perform(put("/books/{id}", existing.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated by Admin")));
    }

    @Test
    @DisplayName("E3: ADMIN can delete a book")
    void testAdminDeleteBook() throws Exception {
        Book existing = bookRepository.findAll().get(0);

        mockMvc.perform(delete("/books/{id}", existing.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // ===== LIBRARIAN TESTS =====

    @Test
    @DisplayName("E4: LIBRARIAN can create a book")
    void testLibrarianCreateBook() throws Exception {
        Book book = new Book("Librarian Book", "Librarian Author", "9780201616224");

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Librarian Book")));
    }

    @Test
    @DisplayName("E5: LIBRARIAN can update a book")
    void testLibrarianUpdateBook() throws Exception {
        Book existing = bookRepository.findAll().get(0);
        Book updated = new Book("Updated by Librarian", "Librarian", "9780132350884");

        mockMvc.perform(put("/books/{id}", existing.getId())
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated by Librarian")));
    }

    @Test
    @DisplayName("E6: LIBRARIAN can delete a book")
    void testLibrarianDeleteBook() throws Exception {
        Book existing = bookRepository.findAll().get(0);

        mockMvc.perform(delete("/books/{id}", existing.getId())
                        .header("Authorization", "Bearer " + librarianToken))
                .andExpect(status().isNoContent());
    }

    // ===== USER TESTS (DENIED) =====

    @Test
    @DisplayName("E7: USER is denied book creation (403)")
    void testUserCreateBookDenied() throws Exception {
        Book book = new Book("User Book", "User Author", "9780201616224");

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("E8: USER is denied book update (403)")
    void testUserUpdateBookDenied() throws Exception {
        Book existing = bookRepository.findAll().get(0);
        Book updated = new Book("User Update", "User", "9780132350884");

        mockMvc.perform(put("/books/{id}", existing.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("E9: USER is denied book deletion (403)")
    void testUserDeleteBookDenied() throws Exception {
        Book existing = bookRepository.findAll().get(0);

        mockMvc.perform(delete("/books/{id}", existing.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("E10: USER can read all books")
    void testUserCanReadBooks() throws Exception {
        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("E11: USER can read a book by ID")
    void testUserCanReadBookById() throws Exception {
        Book existing = bookRepository.findAll().get(0);

        mockMvc.perform(get("/books/{id}", existing.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Existing Book")));
    }

    // ===== UNAUTHENTICATED TESTS =====

    @Test
    @DisplayName("E12: Unauthenticated request to GET /books returns 401")
    void testUnauthenticatedGetBooks() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("E13: Unauthenticated request to POST /books returns 401")
    void testUnauthenticatedCreateBook() throws Exception {
        Book book = new Book("Test", "Author", "9780201616224");

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isUnauthorized());
    }
}
