package io.spring.infrastructure.jpa.repository;

import io.spring.infrastructure.jpa.entity.JpaArticleTagRelation;
import io.spring.infrastructure.jpa.entity.JpaArticleTagRelationId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaArticleTagRelationRepository
    extends JpaRepository<JpaArticleTagRelation, JpaArticleTagRelationId> {
  List<JpaArticleTagRelation> findByIdArticleId(String articleId);
}
