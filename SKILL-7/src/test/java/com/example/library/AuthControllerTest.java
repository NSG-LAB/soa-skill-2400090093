package com.example.library;

import com.example.library.dto.LoginRequest;
import com.example.library.dto.RegisterRequest;
import com.example.library.entity.AuthProvider;
import com.example.library.entity.Role;
import com.example.library.entity.User;
import com.example.library.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("C1: Register a new user successfully")
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "password123", "test@example.com");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.role", is("USER")));

        // Verify user persisted with hashed password
        User saved = userRepository.findByUsername("testuser").orElseThrow();
        assertEquals(Role.USER, saved.getRole());
        assertEquals(AuthProvider.LOCAL, saved.getAuthProvider());
        assertTrue(passwordEncoder.matches("password123", saved.getPassword()));
    }

    @Test
    @DisplayName("C2: Reject duplicate username registration")
    void testRegister_DuplicateUsername() throws Exception {
        // Create first user
        User existing = new User("testuser", passwordEncoder.encode("pass"), "a@b.com", Role.USER, AuthProvider.LOCAL);
        userRepository.save(existing);

        RegisterRequest request = new RegisterRequest("testuser", "password123", "other@example.com");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already taken")));
    }

    @Test
    @DisplayName("C3: Login with valid credentials")
    void testLogin_Success() throws Exception {
        // Create user
        User user = new User("admin", passwordEncoder.encode("admin123"), "admin@test.com", Role.ADMIN, AuthProvider.LOCAL);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("admin", "admin123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.role", is("ADMIN")));
    }

    @Test
    @DisplayName("C4: Reject login with invalid password")
    void testLogin_InvalidPassword() throws Exception {
        User user = new User("admin", passwordEncoder.encode("admin123"), "admin@test.com", Role.ADMIN, AuthProvider.LOCAL);
        userRepository.save(user);

        LoginRequest request = new LoginRequest("admin", "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid")));
    }

    @Test
    @DisplayName("C5: Reject login with nonexistent username")
    void testLogin_NonexistentUser() throws Exception {
        LoginRequest request = new LoginRequest("nobody", "password");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid")));
    }

    @Test
    @DisplayName("C6: Register with missing required fields")
    void testRegister_MissingFields() throws Exception {
        // Missing username
        String json = "{\"password\":\"pass123456\"}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
