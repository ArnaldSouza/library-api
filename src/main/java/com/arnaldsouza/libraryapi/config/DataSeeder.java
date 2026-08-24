package com.arnaldsouza.libraryapi.config;

import com.arnaldsouza.libraryapi.entity.Role;
import com.arnaldsouza.libraryapi.entity.User;
import com.arnaldsouza.libraryapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin12345";

    @Bean
    public CommandLineRunner seedAdminUser(UserRepository userRepository,
                                           PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername(DEFAULT_ADMIN_USERNAME).isPresent()) {
                return;
            }

            User admin = new User();
            admin.setUsername(DEFAULT_ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);

            log.info("Default admin user created. Change the password before any real deployment.");
        };
    }
}