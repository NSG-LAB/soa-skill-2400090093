package com.example.library;

import com.example.library.entity.AuthProvider;
import com.example.library.entity.Role;
import com.example.library.entity.User;
import com.example.library.repository.UserRepository;
import com.example.library.security.JwtService;
import com.example.library.security.OAuth2SuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class GitHubOAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("F1: OAuth success handler creates new user with USER role")
    void testOAuthCreatesNewUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication auth = createMockOAuth2Authentication(12345, "githubuser", "github@example.com");

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        // Verify user was created
        Optional<User> user = userRepository.findByGithubId(12345L);
        assertTrue(user.isPresent());
        assertEquals("githubuser", user.get().getUsername());
        assertEquals(Role.USER, user.get().getRole()); // Always USER
        assertEquals(AuthProvider.GITHUB, user.get().getAuthProvider());
        assertNull(user.get().getPassword()); // No password for OAuth users

        // Verify redirect contains token
        String redirectUrl = response.getRedirectedUrl();
        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("token="));
        assertTrue(redirectUrl.contains("username=githubuser"));
        assertTrue(redirectUrl.contains("role=USER"));
    }

    @Test
    @DisplayName("F2: OAuth success handler recognizes existing user")
    void testOAuthRecognizesExistingUser() throws Exception {
        // Create existing GitHub user
        User existing = new User();
        existing.setUsername("existingghuser");
        existing.setGithubId(12345L);
        existing.setRole(Role.LIBRARIAN); // Previously promoted
        existing.setAuthProvider(AuthProvider.GITHUB);
        userRepository.save(existing);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication auth = createMockOAuth2Authentication(12345, "existingghuser", null);

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        // Verify no duplicate user created
        assertEquals(1, userRepository.count());

        // Verify redirect contains LIBRARIAN role (preserved)
        String redirectUrl = response.getRedirectedUrl();
        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("role=LIBRARIAN"));
    }

    @Test
    @DisplayName("F3: OAuth handles missing email gracefully")
    void testOAuthHandlesNullEmail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication auth = createMockOAuth2Authentication(99999, "privateuser", null);

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        Optional<User> user = userRepository.findByGithubId(99999L);
        assertTrue(user.isPresent());
        assertNull(user.get().getEmail()); // null email is safe
    }

    @Test
    @DisplayName("F4: OAuth handles username conflict by appending suffix")
    void testOAuthHandlesUsernameConflict() throws Exception {
        // Create a local user with the same username
        User localUser = new User("conflictuser", "hashedpass", "local@test.com", Role.USER, AuthProvider.LOCAL);
        userRepository.save(localUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication auth = createMockOAuth2Authentication(77777, "conflictuser", "gh@test.com");

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        // Both users should exist
        assertEquals(2, userRepository.count());

        // GitHub user should have modified username
        Optional<User> ghUser = userRepository.findByGithubId(77777L);
        assertTrue(ghUser.isPresent());
        assertTrue(ghUser.get().getUsername().contains("conflictuser"));
        assertNotEquals("conflictuser", ghUser.get().getUsername());
    }

    @Test
    @DisplayName("F5: Default OAuth user cannot perform privileged operations")
    void testOAuthUserCannotWrite() throws Exception {
        // Simulate OAuth user creation
        User oauthUser = new User();
        oauthUser.setUsername("oauthuser");
        oauthUser.setGithubId(55555L);
        oauthUser.setRole(Role.USER);
        oauthUser.setAuthProvider(AuthProvider.GITHUB);
        userRepository.save(oauthUser);

        String token = jwtService.generateToken("oauthuser", "USER");

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"Test\",\"author\":\"Author\",\"isbn\":\"9780132350884\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("F6: Promoted OAuth user (ADMIN) can perform privileged operations")
    void testPromotedOAuthUserCanWrite() throws Exception {
        User oauthAdmin = new User();
        oauthAdmin.setUsername("oauthadmin");
        oauthAdmin.setGithubId(88888L);
        oauthAdmin.setRole(Role.ADMIN);
        oauthAdmin.setAuthProvider(AuthProvider.GITHUB);
        userRepository.save(oauthAdmin);

        String token = jwtService.generateToken("oauthadmin", "ADMIN");

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"OAuth Admin Book\",\"author\":\"Admin\",\"isbn\":\"9780132350884\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("F7: OAuth success handler rejects missing GitHub ID")
    void testOAuthRejectsMissingGithubId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulate an OAuth response with missing GitHub ID
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("login", "someuser");
        // Deliberately omit "id" to simulate invalid OAuth response

        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attributes,
                "login"
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oAuth2User);

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        // Should redirect with error, not create a user
        String redirectUrl = response.getRedirectedUrl();
        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("error=oauth_missing_id"));
        assertEquals(0, userRepository.count());
    }

    // ===== HELPER =====

    private Authentication createMockOAuth2Authentication(int githubId, String login, String email) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", githubId);
        attributes.put("login", login);
        attributes.put("email", email);

        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attributes,
                "login"
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oAuth2User);
        return auth;
    }
}
