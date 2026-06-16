package io.spring.infrastructure.jpa.entity;

import io.spring.core.favorite.ArticleFavorite;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "article_favorites")
public class JpaArticleFavorite {
  @EmbeddedId private JpaArticleFavoriteId id;

  protected JpaArticleFavorite() {}

  private JpaArticleFavorite(JpaArticleFavoriteId id) {
    this.id = id;
  }

  public static JpaArticleFavorite fromDomain(ArticleFavorite articleFavorite) {
    return new JpaArticleFavorite(
        new JpaArticleFavoriteId(articleFavorite.getArticleId(), articleFavorite.getUserId()));
  }

  public ArticleFavorite toDomain() {
    return new ArticleFavorite(id.getArticleId(), id.getUserId());
  }
}
