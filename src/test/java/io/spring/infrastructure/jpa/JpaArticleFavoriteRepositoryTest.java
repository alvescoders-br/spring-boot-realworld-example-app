package io.spring.infrastructure.jpa;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.favorite.ArticleFavorite;
import io.spring.infrastructure.jpa.entity.JpaArticleFavorite;
import io.spring.infrastructure.jpa.entity.JpaArticleFavoriteId;
import io.spring.infrastructure.jpa.repository.SpringDataJpaArticleFavoriteRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JpaArticleFavoriteRepositoryTest {
  @Mock private SpringDataJpaArticleFavoriteRepository springDataArticleFavoriteRepository;

  private JpaArticleFavoriteRepository articleFavoriteRepository;

  @BeforeEach
  public void setUp() {
    articleFavoriteRepository = new JpaArticleFavoriteRepository(springDataArticleFavoriteRepository);
  }

  @Test
  public void should_save_favorite_once() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");
    JpaArticleFavoriteId favoriteId = new JpaArticleFavoriteId("article-id", "user-id");
    when(springDataArticleFavoriteRepository.existsById(favoriteId)).thenReturn(false);

    articleFavoriteRepository.save(favorite);

    verify(springDataArticleFavoriteRepository)
        .save(argThat(jpaFavorite -> jpaFavorite.toDomain().equals(favorite)));
  }

  @Test
  public void should_not_duplicate_existing_favorite() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");
    JpaArticleFavoriteId favoriteId = new JpaArticleFavoriteId("article-id", "user-id");
    when(springDataArticleFavoriteRepository.existsById(favoriteId)).thenReturn(true);

    articleFavoriteRepository.save(favorite);

    verify(springDataArticleFavoriteRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void should_find_and_remove_favorite() {
    ArticleFavorite favorite = new ArticleFavorite("article-id", "user-id");
    JpaArticleFavoriteId favoriteId = new JpaArticleFavoriteId("article-id", "user-id");
    when(springDataArticleFavoriteRepository.findById(favoriteId))
        .thenReturn(Optional.of(JpaArticleFavorite.fromDomain(favorite)));

    Optional<ArticleFavorite> result = articleFavoriteRepository.find("article-id", "user-id");
    articleFavoriteRepository.remove(favorite);

    Assertions.assertEquals(Optional.of(favorite), result);
    verify(springDataArticleFavoriteRepository).deleteById(favoriteId);
  }
}
