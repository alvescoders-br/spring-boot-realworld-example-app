package io.spring.infrastructure.jpa;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.infrastructure.jpa.entity.JpaArticleFavorite;
import io.spring.infrastructure.jpa.entity.JpaArticleFavoriteId;
import io.spring.infrastructure.jpa.repository.SpringDataJpaArticleFavoriteRepository;
import io.spring.infrastructure.mybatis.mapper.ArticleFavoriteMapper;
import io.spring.infrastructure.repository.MyBatisArticleFavoriteRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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

  @Test
  public void should_select_jpa_favorite_repository_in_spring_context_for_postgres_profile() {
    try (AnnotationConfigApplicationContext context = repositoryContext("postgres")) {
      Map<String, ArticleFavoriteRepository> repositories =
          context.getBeansOfType(ArticleFavoriteRepository.class);

      Assertions.assertEquals(1, repositories.size());
      Assertions.assertInstanceOf(
          JpaArticleFavoriteRepository.class, repositories.values().iterator().next());
      Assertions.assertFalse(context.containsBean("myBatisArticleFavoriteRepository"));
    }
  }

  @Test
  public void should_select_mybatis_favorite_repository_without_postgres_profile() {
    try (AnnotationConfigApplicationContext context = repositoryContext()) {
      Map<String, ArticleFavoriteRepository> repositories =
          context.getBeansOfType(ArticleFavoriteRepository.class);

      Assertions.assertEquals(1, repositories.size());
      Assertions.assertInstanceOf(
          MyBatisArticleFavoriteRepository.class, repositories.values().iterator().next());
      Assertions.assertFalse(context.containsBean("jpaArticleFavoriteRepository"));
    }
  }

  private AnnotationConfigApplicationContext repositoryContext(String... activeProfiles) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment().setActiveProfiles(activeProfiles);
    context.registerBean(
        SpringDataJpaArticleFavoriteRepository.class,
        () -> mock(SpringDataJpaArticleFavoriteRepository.class));
    context.registerBean(ArticleFavoriteMapper.class, () -> mock(ArticleFavoriteMapper.class));
    context.register(JpaArticleFavoriteRepository.class, MyBatisArticleFavoriteRepository.class);
    context.refresh();
    return context;
  }
}
