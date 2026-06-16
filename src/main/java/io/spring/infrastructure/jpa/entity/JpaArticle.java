package io.spring.infrastructure.jpa.entity;

import io.spring.core.article.Article;
import io.spring.core.article.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "articles")
public class JpaArticle {
  @Id
  @Column(length = 255)
  private String id;

  @Column(name = "user_id", length = 255)
  private String userId;

  @Column(length = 255, unique = true)
  private String slug;

  @Column(length = 255)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(columnDefinition = "text")
  private String body;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected JpaArticle() {}

  private JpaArticle(
      String id,
      String userId,
      String slug,
      String title,
      String description,
      String body,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.userId = userId;
    this.slug = slug;
    this.title = title;
    this.description = description;
    this.body = body;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static JpaArticle fromDomain(Article article) {
    return new JpaArticle(
        article.getId(),
        article.getUserId(),
        article.getSlug(),
        article.getTitle(),
        article.getDescription(),
        article.getBody(),
        article.getCreatedAt(),
        article.getUpdatedAt());
  }

  public String getId() {
    return id;
  }

  public Article toDomain(List<Tag> tags) {
    return Article.restored(id, userId, slug, title, description, body, tags, createdAt, updatedAt);
  }
}
