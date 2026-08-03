package dev.modsweb.repository;

import dev.modsweb.entity.ModDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModDependencyRepository extends JpaRepository<ModDependency, Long> {

    List<ModDependency> findByModId(Long modId);

    void deleteByModId(Long modId);
}
