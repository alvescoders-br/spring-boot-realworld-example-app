package io.spring.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class JpaArticleTagRelationId implements Serializable {
  @Column(name = "article_id", length = 255, nullable = false)
  private String articleId;

  @Column(name = "tag_id", length = 255, nullable = false)
  private String tagId;

  protected JpaArticleTagRelationId() {}

  public JpaArticleTagRelationId(String articleId, String tagId) {
    this.articleId = articleId;
    this.tagId = tagId;
  }

  public String getArticleId() {
    return articleId;
  }

  public String getTagId() {
    return tagId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof JpaArticleTagRelationId that)) {
      return false;
    }
    return Objects.equals(articleId, that.articleId) && Objects.equals(tagId, that.tagId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(articleId, tagId);
  }
}
