package io.spring.infrastructure.jpa;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.infrastructure.jpa.entity.JpaComment;
import io.spring.infrastructure.jpa.repository.SpringDataJpaCommentRepository;
import io.spring.infrastructure.mybatis.mapper.CommentMapper;
import io.spring.infrastructure.repository.MyBatisCommentRepository;
import java.time.Instant;
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
public class JpaCommentRepositoryTest {
  @Mock private SpringDataJpaCommentRepository springDataCommentRepository;

  private JpaCommentRepository commentRepository;

  @BeforeEach
  public void setUp() {
    commentRepository = new JpaCommentRepository(springDataCommentRepository);
  }

  @Test
  public void should_save_comment_without_updated_at_domain_field() {
    Comment comment = restoredComment("comment-id", "comment body", "user-id", "article-id");

    commentRepository.save(comment);

    verify(springDataCommentRepository)
        .save(
            argThat(
                jpaComment -> {
                  Comment savedComment = jpaComment.toDomain();
                  return savedComment.equals(comment)
                      && savedComment.getBody().equals(comment.getBody())
                      && savedComment.getUserId().equals(comment.getUserId())
                      && savedComment.getArticleId().equals(comment.getArticleId())
                      && savedComment.getCreatedAt().equals(comment.getCreatedAt());
                }));
  }

  @Test
  public void should_find_comment_by_article_id_and_comment_id() {
    Comment comment = restoredComment("comment-id", "comment body", "user-id", "article-id");
    when(springDataCommentRepository.findByArticleIdAndId(comment.getArticleId(), comment.getId()))
        .thenReturn(Optional.of(JpaComment.fromDomain(comment)));

    Optional<Comment> result = commentRepository.findById(comment.getArticleId(), comment.getId());

    Assertions.assertEquals(Optional.of(comment), result);
    Assertions.assertEquals(comment.getArticleId(), result.orElseThrow().getArticleId());
    Assertions.assertEquals(comment.getUserId(), result.orElseThrow().getUserId());
  }

  @Test
  public void should_remove_comment_by_id() {
    Comment comment = restoredComment("comment-id", "comment body", "user-id", "article-id");

    commentRepository.remove(comment);

    verify(springDataCommentRepository).deleteById(comment.getId());
  }

  @Test
  public void should_select_jpa_comment_repository_in_spring_context_for_postgres_profile() {
    try (AnnotationConfigApplicationContext context = repositoryContext("postgres")) {
      Map<String, CommentRepository> repositories = context.getBeansOfType(CommentRepository.class);

      Assertions.assertEquals(1, repositories.size());
      Assertions.assertInstanceOf(JpaCommentRepository.class, repositories.values().iterator().next());
      Assertions.assertFalse(context.containsBean("myBatisCommentRepository"));
    }
  }

  @Test
  public void should_select_mybatis_comment_repository_without_postgres_profile() {
    try (AnnotationConfigApplicationContext context = repositoryContext()) {
      Map<String, CommentRepository> repositories = context.getBeansOfType(CommentRepository.class);

      Assertions.assertEquals(1, repositories.size());
      Assertions.assertInstanceOf(MyBatisCommentRepository.class, repositories.values().iterator().next());
      Assertions.assertFalse(context.containsBean("jpaCommentRepository"));
    }
  }

  private AnnotationConfigApplicationContext repositoryContext(String... activeProfiles) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getEnvironment().setActiveProfiles(activeProfiles);
    context.registerBean(
        SpringDataJpaCommentRepository.class, () -> mock(SpringDataJpaCommentRepository.class));
    context.registerBean(CommentMapper.class, () -> mock(CommentMapper.class));
    context.register(JpaCommentRepository.class, MyBatisCommentRepository.class);
    context.refresh();
    return context;
  }

  private Comment restoredComment(String id, String body, String userId, String articleId) {
    return Comment.restored(id, body, userId, articleId, Instant.parse("2026-06-16T12:30:45.000Z"));
  }
}
