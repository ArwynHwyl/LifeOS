package com.example.demo.repository;

import com.example.demo.entity.AppMeta;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppMetaRepository extends JpaRepository<AppMeta, Long> {

    Optional<AppMeta> findByName(String name);
}
