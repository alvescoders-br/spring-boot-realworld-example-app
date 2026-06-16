package io.spring.infrastructure.jpa.repository;

import io.spring.infrastructure.jpa.entity.JpaUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaUserRepository extends JpaRepository<JpaUser, String> {
  Optional<JpaUser> findByUsername(String username);

  Optional<JpaUser> findByEmail(String email);
}
