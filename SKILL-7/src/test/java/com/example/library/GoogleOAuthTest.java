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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
public class GoogleOAuthTest {

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
    @DisplayName("G1: Google OAuth success handler creates new user with USER role and GOOGLE provider")
    void testGoogleOAuthCreatesNewUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication auth = createMockGoogleOAuth2Authentication("109876543210987654321", "Google User", "guser@gmail.com");

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        // Verify user was created
        Optional<User> user = userRepository.findByGoogleId("109876543210987654321");
        assertTrue(user.isPresent());
        assertEquals("guser", user.get().getUsername());
        assertEquals("guser@gmail.com", user.get().getEmail());
        assertEquals(Role.USER, user.get().getRole()); // Always USER
        assertEquals(AuthProvider.GOOGLE, user.get().getAuthProvider());
        assertNull(user.get().getPassword()); // No password for OAuth users

        // Verify redirect contains token
        String redirectUrl = response.getRedirectedUrl();
        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("token="));
        assertTrue(redirectUrl.contains("username=guser"));
        assertTrue(redirectUrl.contains("role=USER"));
    }

    @Test
    @DisplayName("G2: Google OAuth success handler recognizes existing user and preserves role")
    void testGoogleOAuthRecognizesExistingUser() throws Exception {
        // Create existing Google user
        User existing = new User();
        existing.setUsername("existingguser");
        existing.setGoogleId("109876543210987654321");
        existing.setRole(Role.LIBRARIAN); // Previously promoted
        existing.setAuthProvider(AuthProvider.GOOGLE);
        userRepository.save(existing);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication auth = createMockGoogleOAuth2Authentication("109876543210987654321", "Existing User", null);

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        // Verify no duplicate user created
        assertEquals(1, userRepository.count());

        // Verify redirect contains LIBRARIAN role (preserved)
        String redirectUrl = response.getRedirectedUrl();
        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("role=LIBRARIAN"));
    }

    @Test
    @DisplayName("G3: Google OAuth handles missing email gracefully by falling back to name")
    void testGoogleOAuthHandlesNullEmail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication auth = createMockGoogleOAuth2Authentication("555555555555555555555", "Alice Wonderland", null);

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        Optional<User> user = userRepository.findByGoogleId("555555555555555555555");
        assertTrue(user.isPresent());
        assertEquals("alice_wonderland", user.get().getUsername());
        assertNull(user.get().getEmail());
    }

    @Test
    @DisplayName("G4: Google OAuth handles username conflict by appending suffix")
    void testGoogleOAuthHandlesUsernameConflict() throws Exception {
        // Create a local user with username matching Google email prefix
        User localUser = new User("conflictuser", "hashedpass", "local@test.com", Role.USER, AuthProvider.LOCAL);
        userRepository.save(localUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication auth = createMockGoogleOAuth2Authentication("999999999999999999999", "Conflict User", "conflictuser@gmail.com");

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        // Both users should exist
        assertEquals(2, userRepository.count());

        // Google user should have modified username
        Optional<User> gUser = userRepository.findByGoogleId("999999999999999999999");
        assertTrue(gUser.isPresent());
        assertTrue(gUser.get().getUsername().contains("conflictuser"));
        assertNotEquals("conflictuser", gUser.get().getUsername());
    }

    @Test
    @DisplayName("G5: Default Google OAuth user cannot perform privileged operations")
    void testGoogleOAuthUserCannotWrite() throws Exception {
        User oauthUser = new User();
        oauthUser.setUsername("googleuser");
        oauthUser.setGoogleId("111111111111111111111");
        oauthUser.setRole(Role.USER);
        oauthUser.setAuthProvider(AuthProvider.GOOGLE);
        userRepository.save(oauthUser);

        String token = jwtService.generateToken("googleuser", "USER");

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"Unauthorized Google Book\",\"author\":\"Google Author\",\"isbn\":\"9780132350884\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("G6: Promoted Google OAuth user (ADMIN) can perform privileged operations")
    void testPromotedGoogleOAuthUserCanWrite() throws Exception {
        User oauthAdmin = new User();
        oauthAdmin.setUsername("googleadmin");
        oauthAdmin.setGoogleId("222222222222222222222");
        oauthAdmin.setRole(Role.ADMIN);
        oauthAdmin.setAuthProvider(AuthProvider.GOOGLE);
        userRepository.save(oauthAdmin);

        String token = jwtService.generateToken("googleadmin", "ADMIN");

        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"title\":\"Google Admin Book\",\"author\":\"Google Admin\",\"isbn\":\"9780132350884\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("G7: Google OAuth success handler rejects missing Google ID (sub)")
    void testGoogleOAuthRejectsMissingSubId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "nosub@gmail.com");
        attributes.put("name", "No Sub User");
        attributes.put("provider", "google");
        // Deliberately omit "sub" to simulate invalid OAuth response

        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attributes,
                "email"
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

    private Authentication createMockGoogleOAuth2Authentication(String sub, String name, String email) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", sub);
        if (name != null) attributes.put("name", name);
        if (email != null) attributes.put("email", email);
        attributes.put("provider", "google");

        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attributes,
                "sub"
        );

        OAuth2AuthenticationToken auth = mock(OAuth2AuthenticationToken.class);
        when(auth.getPrincipal()).thenReturn(oAuth2User);
        when(auth.getAuthorizedClientRegistrationId()).thenReturn("google");
        return auth;
    }
}
