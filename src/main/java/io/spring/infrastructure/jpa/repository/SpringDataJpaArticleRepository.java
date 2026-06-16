package io.spring.infrastructure.jpa.repository;

import io.spring.infrastructure.jpa.entity.JpaArticle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaArticleRepository extends JpaRepository<JpaArticle, String> {
  Optional<JpaArticle> findBySlug(String slug);
}
