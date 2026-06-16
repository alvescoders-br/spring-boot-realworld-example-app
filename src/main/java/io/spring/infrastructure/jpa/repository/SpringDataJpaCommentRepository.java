package io.spring.infrastructure.jpa.repository;

import io.spring.infrastructure.jpa.entity.JpaComment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaCommentRepository extends JpaRepository<JpaComment, String> {
  Optional<JpaComment> findByArticleIdAndId(String articleId, String id);
}
