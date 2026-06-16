package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleCommandServiceTest {
  @Mock private ArticleRepository articleRepository;

  @Test
  void should_update_article_and_persist_changes() {
    User author = new User("author@test.com", "author", "password", "bio", "image");
    Article article = new Article("old title", "old desc", "old body", List.of("java"), author.getId());
    ArticleCommandService service = new ArticleCommandService(articleRepository);

    Article updated =
        service.updateArticle(
            article, new UpdateArticleParam("new title", "new body", "new description"));

    assertEquals(article, updated);
    assertEquals("new title", updated.getTitle());
    assertEquals("new-title", updated.getSlug());
    assertEquals("new description", updated.getDescription());
    assertEquals("new body", updated.getBody());
    verify(articleRepository).save(article);
  }
}
