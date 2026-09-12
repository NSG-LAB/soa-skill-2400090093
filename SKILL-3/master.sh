#!/usr/bin/env bash

set -e

PROJECT="user-registration-service"

echo "=========================================="
echo " Skill Experiment 3"
echo " User Registration & Login System"
echo "=========================================="

echo "[1/8] Creating project..."

mkdir -p "$PROJECT"
cd "$PROJECT"

mkdir -p src/main/java/com/example/userservice/{controller,entity,repository,service,exception}
mkdir -p src/main/resources

echo "[2/8] Creating pom.xml..."

cat > pom.xml <<'EOF'
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>user-registration-service</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>

        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
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

echo "[3/8] Creating application.properties..."

cat > src/main/resources/application.properties <<'EOF'
spring.application.name=user-registration-service

server.port=8081

spring.datasource.url=jdbc:mysql://localhost:3306/userdb?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
EOF

echo "[4/8] Creating main application..."

cat > src/main/java/com/example/userservice/UserRegistrationApplication.java <<'EOF'
package com.example.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserRegistrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserRegistrationApplication.class, args);
    }
}
EOF

echo "[5/8] Creating User entity..."

cat > src/main/java/com/example/userservice/entity/User.java <<'EOF'
package com.example.userservice.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
EOF

echo "[6/8] Creating repository..."

cat > src/main/java/com/example/userservice/repository/UserRepository.java <<'EOF'
package com.example.userservice.repository;

import com.example.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);
}
EOF

echo "[7/8] Creating service and controller..."

cat > src/main/java/com/example/userservice/service/UserService.java <<'EOF'
package com.example.userservice.service;

import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String register(User user) {

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        userRepository.save(user);

        return "User registered successfully";
    }

    public String login(String username, String password) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        return "Login successful";
    }
}
EOF

cat > src/main/java/com/example/userservice/controller/UserController.java <<'EOF'
package com.example.userservice.controller;

import com.example.userservice.entity.User;
import com.example.userservice.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody User user) {

        try {
            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            userService.register(user)
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {

            String result = userService.login(
                    request.username(),
                    request.password()
            );

            return ResponseEntity.ok(
                    Map.of("message", result)
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .status(401)
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }
}
EOF

echo "[8/8] Creating global validation handler..."

cat > src/main/java/com/example/userservice/exception/GlobalExceptionHandler.java <<'EOF'
package com.example.userservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(
            MethodArgumentNotValidException ex) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "error",
                        "Invalid input: username, email and password are required"
                ));
    }
}
EOF

echo ""
echo "=========================================="
echo " Project created successfully!"
echo "=========================================="

echo ""
echo "Checking Java..."
java -version

echo ""
echo "Checking Maven..."
mvn -version

echo ""
echo "Building application..."
mvn clean package -DskipTests

echo ""
echo "=========================================="
echo " BUILD SUCCESSFUL"
echo "=========================================="

echo ""
echo "Starting User Registration Service..."
echo "API: http://localhost:8081"
echo ""

java -jar target/user-registration-service-1.0.0.jar
