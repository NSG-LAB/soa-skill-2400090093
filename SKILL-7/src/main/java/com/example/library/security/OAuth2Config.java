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
    private String clientId;

    @Value("${github.client.secret:}")
    private String clientSecret;

    @Value("${github.client.callback-url:http://localhost:8080/login/oauth2/code/github}")
    private String callbackUrl;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        String effectiveClientId = (clientId != null && !clientId.isBlank()) ? clientId : "dummy-github-client-id";
        String effectiveClientSecret = (clientSecret != null && !clientSecret.isBlank()) ? clientSecret : "dummy-github-client-secret";

        if (clientId == null || clientId.isBlank()) {
            logger.warn("GitHub OAuth running with dummy credentials (GITHUB_CLIENT_ID not set)");
        } else {
            logger.info("GitHub OAuth enabled");
        }

        ClientRegistration github = ClientRegistration.withRegistrationId("github")
                .clientId(effectiveClientId)
                .clientSecret(effectiveClientSecret)
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("login")
                .redirectUri(callbackUrl)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientName("GitHub")
                .build();

        return new InMemoryClientRegistrationRepository(github);
    }
}
