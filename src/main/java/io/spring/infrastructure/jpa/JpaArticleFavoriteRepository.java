package io.spring.infrastructure.jpa;

import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.infrastructure.jpa.entity.JpaArticleFavorite;
import io.spring.infrastructure.jpa.entity.JpaArticleFavoriteId;
import io.spring.infrastructure.jpa.repository.SpringDataJpaArticleFavoriteRepository;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class JpaArticleFavoriteRepository implements ArticleFavoriteRepository {
  private final SpringDataJpaArticleFavoriteRepository articleFavoriteRepository;

  public JpaArticleFavoriteRepository(
      SpringDataJpaArticleFavoriteRepository articleFavoriteRepository) {
    this.articleFavoriteRepository = articleFavoriteRepository;
  }

  @Override
  @Transactional
  public void save(ArticleFavorite articleFavorite) {
    JpaArticleFavoriteId favoriteId = favoriteId(articleFavorite);
    if (articleFavoriteRepository.existsById(favoriteId)) {
      return;
    }
    articleFavoriteRepository.save(JpaArticleFavorite.fromDomain(articleFavorite));
  }

  @Override
  public Optional<ArticleFavorite> find(String articleId, String userId) {
    return articleFavoriteRepository
        .findById(new JpaArticleFavoriteId(articleId, userId))
        .map(JpaArticleFavorite::toDomain);
  }

  @Override
  @Transactional
  public void remove(ArticleFavorite favorite) {
    articleFavoriteRepository.deleteById(favoriteId(favorite));
  }

  private JpaArticleFavoriteId favoriteId(ArticleFavorite favorite) {
    return new JpaArticleFavoriteId(favorite.getArticleId(), favorite.getUserId());
  }
}
