package io.spring.infrastructure.jpa.repository;

import io.spring.infrastructure.jpa.entity.JpaTag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaTagRepository extends JpaRepository<JpaTag, String> {
  Optional<JpaTag> findByName(String name);
}
