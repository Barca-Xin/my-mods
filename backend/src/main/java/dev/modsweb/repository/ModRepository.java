package dev.modsweb.repository;

import dev.modsweb.entity.Mod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModRepository extends JpaRepository<Mod, Long> {

    Optional<Mod> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
