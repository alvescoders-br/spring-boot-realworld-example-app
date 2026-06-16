package io.spring.infrastructure.jpa;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("postgres")
@Transactional
@EnabledIfEnvironmentVariable(named = "REALWORLD_POSTGRES_URL", matches = "jdbc:postgresql:.*")
public class JpaArticleRepositoryPostgresIntegrationTest {
  @Autowired private ArticleRepository articleRepository;

  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;

  @Autowired private UserRepository userRepository;

  @Test
  public void should_validate_schema_and_execute_article_and_favorite_repositories() {
    Assertions.assertInstanceOf(JpaArticleRepository.class, articleRepository);
    Assertions.assertInstanceOf(JpaArticleFavoriteRepository.class, articleFavoriteRepository);

    String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    User author =
        new User(
            "article-author-" + uniqueSuffix + "@example.com",
            "article-author-" + uniqueSuffix,
            "encoded-password",
            "author bio",
            "https://example.com/article-author-" + uniqueSuffix + ".png");
    User reader =
        new User(
            "article-reader-" + uniqueSuffix + "@example.com",
            "article-reader-" + uniqueSuffix,
            "encoded-password",
            "reader bio",
            "https://example.com/article-reader-" + uniqueSuffix + ".png");
    userRepository.save(author);
    userRepository.save(reader);

    Article article =
        new Article(
            "JPA Article " + uniqueSuffix,
            "original description",
            "original body",
            List.of("java", "spring", "java"),
            author.getId());
    articleRepository.save(article);

    Optional<Article> createdArticle = articleRepository.findBySlug(article.getSlug());
    Assertions.assertTrue(createdArticle.isPresent());
    Assertions.assertEquals(article, createdArticle.get());
    Assertions.assertTrue(createdArticle.get().getTags().contains(new Tag("java")));
    Assertions.assertTrue(createdArticle.get().getTags().contains(new Tag("spring")));

    Article updateRequest =
        Article.restored(
            article.getId(),
            author.getId(),
            Article.toSlug("JPA Article Updated " + uniqueSuffix),
            "JPA Article Updated " + uniqueSuffix,
            "",
            "",
            article.getTags(),
            article.getCreatedAt(),
            article.getUpdatedAt());
    articleRepository.save(updateRequest);

    Optional<Article> updatedArticle = articleRepository.findBySlug(updateRequest.getSlug());
    Assertions.assertTrue(updatedArticle.isPresent());
    Assertions.assertEquals("JPA Article Updated " + uniqueSuffix, updatedArticle.get().getTitle());
    Assertions.assertEquals("original description", updatedArticle.get().getDescription());
    Assertions.assertEquals("original body", updatedArticle.get().getBody());

    ArticleFavorite favorite = new ArticleFavorite(article.getId(), reader.getId());
    articleFavoriteRepository.save(favorite);
    articleFavoriteRepository.save(favorite);
    Assertions.assertEquals(Optional.of(favorite), articleFavoriteRepository.find(article.getId(), reader.getId()));

    articleFavoriteRepository.remove(favorite);
    Assertions.assertTrue(articleFavoriteRepository.find(article.getId(), reader.getId()).isEmpty());

    articleRepository.remove(article);
    Assertions.assertTrue(articleRepository.findById(article.getId()).isEmpty());
  }
}
