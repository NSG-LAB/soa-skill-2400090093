package com.example.library;

import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import com.example.library.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class JwtSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private JwtService jwtService;


    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        bookRepository.save(new Book("Test Book", "Test Author", "9780132350884"));
    }

    @Test
    @DisplayName("D1: Access with valid JWT token")
    void testValidToken() throws Exception {
        String token = jwtService.generateToken("testuser", "USER");

        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("D2: Reject request with missing token (401)")
    void testMissingToken() throws Exception {
        mockMvc.perform(get("/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("D3: Reject request with expired token (401)")
    void testExpiredToken() throws Exception {
        // Create a token that expired 1 hour ago
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D4: Reject request with malformed token (401)")
    void testMalformedToken() throws Exception {
        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer not.a.valid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D5: Reject request with tampered token (401)")
    void testTamperedToken() throws Exception {
        String validToken = jwtService.generateToken("testuser", "USER");
        // Tamper with the payload
        String[] parts = validToken.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x" + "." + parts[2];

        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D6: Reject request with invalid signature (401)")
    void testInvalidSignature() throws Exception {
        // Sign with a different key
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode("YW5vdGhlciB2ZXJ5IGxvbmcgc2VjcmV0IGtleSB0aGF0IGlzIGRpZmZlcmVudA=="));

        String tokenWithWrongKey = Jwts.builder()
                .subject("testuser")
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer " + tokenWithWrongKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("D7: Reject request with empty Bearer token (401)")
    void testEmptyBearerToken() throws Exception {
        mockMvc.perform(get("/books")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }
}
