package io.spring.infrastructure.jpa.entity;

import io.spring.core.comment.Comment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class JpaComment {
  @Id
  @Column(length = 255)
  private String id;

  @Column(columnDefinition = "text")
  private String body;

  @Column(name = "user_id", length = 255)
  private String userId;

  @Column(name = "article_id", length = 255)
  private String articleId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected JpaComment() {}

  private JpaComment(String id, String body, String userId, String articleId, Instant createdAt) {
    this.id = id;
    this.body = body;
    this.userId = userId;
    this.articleId = articleId;
    this.createdAt = createdAt;
  }

  public static JpaComment fromDomain(Comment comment) {
    return new JpaComment(
        comment.getId(),
        comment.getBody(),
        comment.getUserId(),
        comment.getArticleId(),
        comment.getCreatedAt());
  }

  public String getId() {
    return id;
  }

  public String getArticleId() {
    return articleId;
  }

  public Comment toDomain() {
    return Comment.restored(id, body, userId, articleId, createdAt);
  }
}
