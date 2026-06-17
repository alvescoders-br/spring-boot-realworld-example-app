package io.spring.infrastructure.jpa.readservice;

import io.spring.application.data.ArticleFavoriteCount;
import io.spring.core.user.User;
import io.spring.infrastructure.mybatis.readservice.ArticleFavoritesReadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Primary
@Profile("postgres")
@Transactional(readOnly = true)
public class JpaArticleFavoritesReadService implements ArticleFavoritesReadService {
  private final EntityManager entityManager;

  public JpaArticleFavoritesReadService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public boolean isUserFavorite(String userId, String articleId) {
    Query query =
        entityManager.createNativeQuery(
            "select count(1) from article_favorites where user_id = :userId and article_id = :articleId");
    query.setParameter("userId", userId);
    query.setParameter("articleId", articleId);
    return ((Number) query.getSingleResult()).intValue() > 0;
  }

  @Override
  public int articleFavoriteCount(String articleId) {
    Query query =
        entityManager.createNativeQuery(
            "select count(1) from article_favorites where article_id = :articleId");
    query.setParameter("articleId", articleId);
    return ((Number) query.getSingleResult()).intValue();
  }

  @Override
  public List<ArticleFavoriteCount> articlesFavoriteCount(List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    Query query =
        entityManager.createNativeQuery(
            "select A.id, count(AF.user_id) as favoriteCount "
                + "from articles A "
                + "left join article_favorites AF on A.id = AF.article_id "
                + "where A.id in (:ids) group by A.id");
    query.setParameter("ids", ids);
    return ((List<Object[]>) query.getResultList())
        .stream()
            .map(
                row ->
                    new ArticleFavoriteCount(
                        JpaReadModelDataMapper.stringValue(row[0]), ((Number) row[1]).intValue()))
            .toList();
  }

  @Override
  public Set<String> userFavorites(List<String> ids, User currentUser) {
    if (ids == null || ids.isEmpty() || currentUser == null) {
      return Set.of();
    }
    Query query =
        entityManager.createNativeQuery(
            "select A.id from articles A "
                + "left join article_favorites AF on A.id = AF.article_id "
                + "where A.id in (:ids) and AF.user_id = :userId");
    query.setParameter("ids", ids);
    query.setParameter("userId", currentUser.getId());
    return ((List<Object>) query.getResultList())
        .stream().map(JpaReadModelDataMapper::stringValue).collect(Collectors.toSet());
  }
}
