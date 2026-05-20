package com.example.demo.config;

import com.example.demo.entity.AppMeta;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.AppMetaRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevSeedConfig {

    @Bean
    CommandLineRunner seedDevData(
            AppMetaRepository appMetaRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            appMetaRepository.findByName("seed.version")
                    .orElseGet(() -> appMetaRepository.save(new AppMeta("seed.version", "v1")));

            if (!userRepository.existsByEmail("admin@lifeos.local")) {
                userRepository.save(new User(
                        "admin@lifeos.local",
                        "admin",
                        passwordEncoder.encode("Admin12345!"),
                        "Admin",
                        "User",
                        UserRole.ROLE_ADMIN,
                        UserStatus.VERIFY
                ));
            }
        };
    }
}
