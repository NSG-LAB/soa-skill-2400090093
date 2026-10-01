package com.example.library.security;

import com.example.library.entity.AuthProvider;
import com.example.library.entity.Role;
import com.example.library.entity.User;
import com.example.library.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public OAuth2SuccessHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // GitHub's stable numeric ID — the only reliable identity key
        Integer githubIdInt = oAuth2User.getAttribute("id");
        if (githubIdInt == null) {
            logger.error("GitHub OAuth response missing 'id' attribute");
            response.sendRedirect("/?error=oauth_missing_id");
            return;
        }
        Long githubId = githubIdInt.longValue();

        String login = oAuth2User.getAttribute("login");
        String email = oAuth2User.getAttribute("email"); // may be null if email is private

        // Look up by GitHub's stable ID, not by email or username
        Optional<User> existingUser = userRepository.findByGithubId(githubId);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            logger.info("Existing GitHub user logged in: {}", user.getUsername());
        } else {
            // Create new user — ensure unique username
            String username = login;
            if (username == null || username.isBlank()) {
                username = "github_" + githubId;
            }
            if (userRepository.existsByUsername(username)) {
                username = username + "_gh_" + githubId;
            }

            user = new User();
            user.setUsername(username);
            user.setEmail(email); // null is safe
            user.setGithubId(githubId);
            user.setRole(Role.USER); // Always USER — never trust client-provided role
            user.setAuthProvider(AuthProvider.GITHUB);
            // No password for OAuth users

            user = userRepository.save(user);
            logger.info("New GitHub user created: {}", user.getUsername());
        }

        // Issue the application's own JWT — never use GitHub access token as JWT
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());

        // Redirect to frontend with token as query parameter
        String redirectUrl = "/?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&username=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8)
                + "&role=" + URLEncoder.encode(user.getRole().name(), StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}
