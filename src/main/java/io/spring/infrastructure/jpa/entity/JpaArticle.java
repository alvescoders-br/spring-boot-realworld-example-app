package io.spring.infrastructure.jpa.entity;

import io.spring.core.article.Article;
import io.spring.core.article.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "articles")
public class JpaArticle {
  @Id
  @Column(length = 255)
  private String id;

  @Column(name = "user_id", length = 255)
  private String userId;

  @Column(length = 255)
  private String slug;

  @Column(length = 255)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(columnDefinition = "text")
  private String body;

  @Column(name = "reading_time")
  private Integer readingTime;

  @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted;

  protected JpaArticle() {}

  private JpaArticle(
      String id,
      String userId,
      String slug,
      String title,
      String description,
      String body,
      Instant createdAt,
      Instant updatedAt,
      boolean deleted) {
    this.id = id;
    this.userId = userId;
    this.slug = slug;
    this.title = title;
    this.description = description;
    this.body = body;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.deleted = deleted;
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
        article.getUpdatedAt(),
        false);
  }

  public String getId() {
    return id;
  }

  public Integer getReadingTime() {
    return readingTime;
  }

  public void setReadingTime(Integer readingTime) {
    this.readingTime = readingTime;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public Article toDomain(List<Tag> tags) {
    return Article.restored(id, userId, slug, title, description, body, tags, createdAt, updatedAt);
  }
}
