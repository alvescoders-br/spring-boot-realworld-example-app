package io.spring.infrastructure.jpa.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "article_tags")
public class JpaArticleTagRelation {
  @EmbeddedId private JpaArticleTagRelationId id;

  protected JpaArticleTagRelation() {}

  public JpaArticleTagRelation(String articleId, String tagId) {
    this.id = new JpaArticleTagRelationId(articleId, tagId);
  }

  public String getArticleId() {
    return id.getArticleId();
  }

  public String getTagId() {
    return id.getTagId();
  }
}
