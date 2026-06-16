package io.spring.infrastructure.jpa;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.service.AuthorizationService;
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
public class JpaCommentRepositoryPostgresIntegrationTest {
  @Autowired private ArticleRepository articleRepository;

  @Autowired private CommentRepository commentRepository;

  @Autowired private UserRepository userRepository;

  @Test
  public void should_validate_schema_and_execute_comment_repository_against_postgres_profile() {
    Assertions.assertInstanceOf(JpaArticleRepository.class, articleRepository);
    Assertions.assertInstanceOf(JpaCommentRepository.class, commentRepository);

    String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    User author =
        new User(
            "comment-author-" + uniqueSuffix + "@example.com",
            "comment-author-" + uniqueSuffix,
            "encoded-password",
            "author bio",
            "https://example.com/comment-author-" + uniqueSuffix + ".png");
    User commenter =
        new User(
            "commenter-" + uniqueSuffix + "@example.com",
            "commenter-" + uniqueSuffix,
            "encoded-password",
            "commenter bio",
            "https://example.com/commenter-" + uniqueSuffix + ".png");
    User stranger =
        new User(
            "stranger-" + uniqueSuffix + "@example.com",
            "stranger-" + uniqueSuffix,
            "encoded-password",
            "stranger bio",
            "https://example.com/stranger-" + uniqueSuffix + ".png");
    userRepository.save(author);
    userRepository.save(commenter);
    userRepository.save(stranger);

    Article article =
        new Article(
            "JPA Comment " + uniqueSuffix,
            "comment description",
            "comment body",
            List.of("java", "comments"),
            author.getId());
    articleRepository.save(article);

    Comment comment = new Comment("postgres comment body", commenter.getId(), article.getId());
    commentRepository.save(comment);

    Optional<Comment> createdComment = commentRepository.findById(article.getId(), comment.getId());
    Optional<Comment> wrongArticleComment =
        commentRepository.findById("wrong-article-id", comment.getId());

    Assertions.assertEquals(Optional.of(comment), createdComment);
    Assertions.assertTrue(wrongArticleComment.isEmpty());
    Assertions.assertTrue(AuthorizationService.canWriteComment(author, article, comment));
    Assertions.assertTrue(AuthorizationService.canWriteComment(commenter, article, comment));
    Assertions.assertFalse(AuthorizationService.canWriteComment(stranger, article, comment));

    commentRepository.remove(comment);

    Assertions.assertTrue(commentRepository.findById(article.getId(), comment.getId()).isEmpty());
  }
}
