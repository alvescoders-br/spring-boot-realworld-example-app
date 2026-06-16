package io.spring.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class JpaArticleFavoriteId implements Serializable {
  @Column(name = "article_id", length = 255, nullable = false)
  private String articleId;

  @Column(name = "user_id", length = 255, nullable = false)
  private String userId;

  protected JpaArticleFavoriteId() {}

  public JpaArticleFavoriteId(String articleId, String userId) {
    this.articleId = articleId;
    this.userId = userId;
  }

  public String getArticleId() {
    return articleId;
  }

  public String getUserId() {
    return userId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof JpaArticleFavoriteId that)) {
      return false;
    }
    return Objects.equals(articleId, that.articleId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(articleId, userId);
  }
}
