package com.example.library.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Configuration
public class OAuth2Config {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Config.class);

    @Value("${github.client.id:}")
    private String githubClientId;

    @Value("${github.client.secret:}")
    private String githubClientSecret;

    @Value("${github.client.callback-url:http://localhost:8080/login/oauth2/code/github}")
    private String githubCallbackUrl;

    @Value("${google.client.id:}")
    private String googleClientId;

    @Value("${google.client.secret:}")
    private String googleClientSecret;

    @Value("${google.client.callback-url:http://localhost:8080/login/oauth2/code/google}")
    private String googleCallbackUrl;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        String effectiveGithubClientId = (githubClientId != null && !githubClientId.isBlank()) ? githubClientId : "dummy-github-client-id";
        String effectiveGithubClientSecret = (githubClientSecret != null && !githubClientSecret.isBlank()) ? githubClientSecret : "dummy-github-client-secret";

        if (githubClientId == null || githubClientId.isBlank()) {
            logger.warn("GitHub OAuth running with dummy credentials (GITHUB_CLIENT_ID not set)");
        } else {
            logger.info("GitHub OAuth enabled");
        }

        ClientRegistration github = ClientRegistration.withRegistrationId("github")
                .clientId(effectiveGithubClientId)
                .clientSecret(effectiveGithubClientSecret)
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("login")
                .redirectUri(githubCallbackUrl)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientName("GitHub")
                .build();

        String effectiveGoogleClientId = (googleClientId != null && !googleClientId.isBlank()) ? googleClientId : "dummy-google-client-id";
        String effectiveGoogleClientSecret = (googleClientSecret != null && !googleClientSecret.isBlank()) ? googleClientSecret : "dummy-google-client-secret";

        if (googleClientId == null || googleClientId.isBlank()) {
            logger.warn("Google OAuth running with dummy credentials (GOOGLE_CLIENT_ID not set)");
        } else {
            logger.info("Google OAuth enabled");
        }

        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(effectiveGoogleClientId)
                .clientSecret(effectiveGoogleClientSecret)
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userNameAttributeName("sub")
                .redirectUri(googleCallbackUrl)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientName("Google")
                .build();

        return new InMemoryClientRegistrationRepository(github, google);
    }
}
