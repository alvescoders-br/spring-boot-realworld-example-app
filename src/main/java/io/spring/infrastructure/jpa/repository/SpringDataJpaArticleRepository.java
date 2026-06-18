package io.spring.infrastructure.jpa.repository;

import io.spring.infrastructure.jpa.entity.JpaArticle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataJpaArticleRepository extends JpaRepository<JpaArticle, String> {
  Optional<JpaArticle> findByIdAndDeletedFalse(String id);

  Optional<JpaArticle> findBySlugAndDeletedFalse(String slug);

  @Modifying
  @Query("update JpaArticle article set article.deleted = true where article.id = :id")
  void softDeleteById(@Param("id") String id);

  @Modifying
  @Query(
      "update JpaArticle article "
          + "set article.readingTime = :readingTime "
          + "where article.id = :id and article.readingTime is null and article.deleted = false")
  int cacheReadingTimeIfAbsent(@Param("id") String id, @Param("readingTime") int readingTime);
}
