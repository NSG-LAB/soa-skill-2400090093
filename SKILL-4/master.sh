#!/usr/bin/env bash

set -e

PROJECT="banking-jwt-api"

echo "=========================================="
echo " Banking JWT Authentication API"
echo "=========================================="

# --------------------------------------------------
# 1. Check prerequisites
# --------------------------------------------------

command -v java >/dev/null 2>&1 || {
    echo "ERROR: Java is not installed."
    exit 1
}

command -v mvn >/dev/null 2>&1 || {
    echo "ERROR: Maven is not installed."
    exit 1
}

echo "Java:"
java -version

echo "Maven:"
mvn -version

# --------------------------------------------------
# 2. Create project
# --------------------------------------------------

rm -rf "$PROJECT"

mkdir -p "$PROJECT"
cd "$PROJECT"

mkdir -p src/main/java/com/bank/security
mkdir -p src/main/java/com/bank/controller
mkdir -p src/main/java/com/bank/service
mkdir -p src/main/java/com/bank/model
mkdir -p src/main/java/com/bank/repository
mkdir -p src/main/java/com/bank/config
mkdir -p src/main/resources
mkdir -p src/test/java/com/bank

echo "Project structure created."

# --------------------------------------------------
# 3. pom.xml
# --------------------------------------------------

cat > pom.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="
         http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
        <relativePath/>
    </parent>

    <groupId>com.bank</groupId>
    <artifactId>banking-jwt-api</artifactId>
    <version>1.0.0</version>

    <name>Banking JWT API</name>
    <description>Secure Banking API using JWT Authentication</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>

        <!-- REST API -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- H2 Database -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
EOF

# --------------------------------------------------
# 4. Main Application
# --------------------------------------------------

cat > src/main/java/com/bank/BankingJwtApplication.java <<'EOF'
package com.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankingJwtApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingJwtApplication.class, args);
    }
}
EOF

# --------------------------------------------------
# 5. User Model
# --------------------------------------------------

cat > src/main/java/com/bank/model/User.java <<'EOF'
package com.bank.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
EOF

# --------------------------------------------------
# 6. User Repository
# --------------------------------------------------

cat > src/main/java/com/bank/repository/UserRepository.java <<'EOF'
package com.bank.repository;

import com.bank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
}
EOF

# --------------------------------------------------
# 7. JWT Service
# --------------------------------------------------

cat > src/main/java/com/bank/service/JwtService.java <<'EOF'
package com.bank.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    public boolean isTokenValid(String token, String username) {

        Claims claims = extractAllClaims(token);

        return claims.getSubject().equals(username)
                && !claims.getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
EOF

# --------------------------------------------------
# 8. Authentication Service
# --------------------------------------------------

cat > src/main/java/com/bank/service/AuthService.java <<'EOF'
package com.bank.service;

import com.bank.model.User;
import com.bank.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String username, String password) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return user;
    }
}
EOF

# --------------------------------------------------
# 9. JWT Authentication Filter
# --------------------------------------------------

cat > src/main/java/com/bank/security/JwtAuthenticationFilter.java <<'EOF'
package com.bank.security;

import com.bank.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            String username = jwtService.extractUsername(token);

            if (username != null &&
                    SecurityContextHolder.getContext()
                            .getAuthentication() == null) {

                if (jwtService.isTokenValid(token, username)) {

                    User userDetails = new User(
                            username,
                            "",
                            Collections.emptyList()
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception e) {

            // Invalid or expired JWT
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
EOF

# --------------------------------------------------
# 10. Security Configuration
# --------------------------------------------------

cat > src/main/java/com/bank/config/SecurityConfig.java <<'EOF'
package com.bank.config;

import com.bank.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        ))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )

                .headers(headers ->
                        headers.frameOptions(frame ->
                                frame.sameOrigin()
                        )
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
EOF

# --------------------------------------------------
# 11. Login Request
# --------------------------------------------------

cat > src/main/java/com/bank/model/LoginRequest.java <<'EOF'
package com.bank.model;

public class LoginRequest {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
EOF

# --------------------------------------------------
# 12. Login Response
# --------------------------------------------------

cat > src/main/java/com/bank/model/LoginResponse.java <<'EOF'
package com.bank.model;

public class LoginResponse {

    private String token;
    private String tokenType;

    public LoginResponse(String token) {
        this.token = token;
        this.tokenType = "Bearer";
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }
}
EOF

# --------------------------------------------------
# 13. Authentication Controller
# --------------------------------------------------

cat > src/main/java/com/bank/controller/AuthController.java <<'EOF'
package com.bank.controller;

import com.bank.model.LoginRequest;
import com.bank.model.LoginResponse;
import com.bank.model.User;
import com.bank.service.AuthService;
import com.bank.service.JwtService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            JwtService jwtService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {

        User user = authService.authenticate(
                request.getUsername(),
                request.getPassword()
        );

        String token = jwtService.generateToken(
                user.getUsername()
        );

        return ResponseEntity.ok(
                new LoginResponse(token)
        );
    }
}
EOF

# --------------------------------------------------
# 14. Account Controller
# --------------------------------------------------

cat > src/main/java/com/bank/controller/AccountController.java <<'EOF'
package com.bank.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/account")
public class AccountController {

    @GetMapping("/details")
    public Map<String, Object> accountDetails(
            Authentication authentication
    ) {

        return Map.of(
                "accountNumber", "XXXX-XXXX-1234",
                "accountHolder", authentication.getName(),
                "accountType", "Savings",
                "balance", 75000.00,
                "currency", "INR",
                "status", "ACTIVE"
        );
    }
}
EOF

# --------------------------------------------------
# 15. Data Initialization
# --------------------------------------------------

cat > src/main/java/com/bank/config/DataInitializer.java <<'EOF'
package com.bank.config;

import com.bank.model.User;
import com.bank.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeUsers(
            UserRepository repository,
            PasswordEncoder passwordEncoder
    ) {

        return args -> {

            if (repository.findByUsername("admin").isEmpty()) {

                User user = new User(
                        "admin",
                        passwordEncoder.encode("admin123")
                );

                repository.save(user);

                System.out.println();
                System.out.println("==========================================");
                System.out.println("Demo User Created");
                System.out.println("Username : admin");
                System.out.println("Password : admin123");
                System.out.println("==========================================");
                System.out.println();
            }
        };
    }
}
EOF

# --------------------------------------------------
# 16. application.properties
# --------------------------------------------------

cat > src/main/resources/application.properties <<'EOF'
spring.application.name=banking-jwt-api

server.port=8080

# H2 Database
spring.datasource.url=jdbc:h2:mem:bankingdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JWT
# Base64 encoded 256-bit secret
jwt.secret=VGhpc0lzQVN1cGVyU2VjdXJlU2VjcmV0S2V5Rm9yQmFua2luZ0pXVFByb2plY3Q=
jwt.expiration=900000

# JSON
spring.jackson.serialization.indent_output=true
EOF

# --------------------------------------------------
# 17. Exception Handler
# --------------------------------------------------

cat > src/main/java/com/bank/config/SecurityExceptionHandler.java <<'EOF'
package com.bank.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {

        if (request.getRequestURI().equals("/auth/login")) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", 401,
                            "error", "Unauthorized",
                            "message", "Invalid username or password"
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "error", "Internal Server Error"
                ));
    }
}
EOF

# --------------------------------------------------
# 18. Build Project
# --------------------------------------------------

echo
echo "=========================================="
echo "Building project..."
echo "=========================================="

mvn clean package -DskipTests

echo
echo "=========================================="
echo "BUILD SUCCESS"
echo "=========================================="

echo
echo "Project: $PROJECT"
echo
echo "Run:"
echo "  cd $PROJECT"
echo "  mvn spring-boot:run"
echo
echo "Server:"
echo "  http://localhost:8080"
echo
echo "Login:"
echo "  POST http://localhost:8080/auth/login"
echo
echo "Protected API:"
echo "  GET http://localhost:8080/account/details"
echo
echo "Demo credentials:"
echo "  username: admin"
echo "  password: admin123"
echo
echo "JWT expiration:"
echo "  15 minutes"
echo
echo "=========================================="
echo "Project created successfully!"
echo "=========================================="
