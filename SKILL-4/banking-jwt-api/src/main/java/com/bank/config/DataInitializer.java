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
