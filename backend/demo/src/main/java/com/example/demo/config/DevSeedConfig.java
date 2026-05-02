package com.example.demo.config;

import com.example.demo.entity.AppMeta;
import com.example.demo.repository.AppMetaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevSeedConfig {

    @Bean
    CommandLineRunner seedAppMeta(AppMetaRepository appMetaRepository) {
        return args -> appMetaRepository.findByName("seed.version")
                .orElseGet(() -> appMetaRepository.save(new AppMeta("seed.version", "v1")));
    }
}
