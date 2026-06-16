package io.spring.infrastructure.jpa;

import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.infrastructure.jpa.entity.JpaComment;
import io.spring.infrastructure.jpa.repository.SpringDataJpaCommentRepository;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class JpaCommentRepository implements CommentRepository {
  private final SpringDataJpaCommentRepository commentRepository;

  public JpaCommentRepository(SpringDataJpaCommentRepository commentRepository) {
    this.commentRepository = commentRepository;
  }

  @Override
  @Transactional
  public void save(Comment comment) {
    commentRepository.save(JpaComment.fromDomain(comment));
  }

  @Override
  public Optional<Comment> findById(String articleId, String id) {
    return commentRepository.findByArticleIdAndId(articleId, id).map(JpaComment::toDomain);
  }

  @Override
  @Transactional
  public void remove(Comment comment) {
    commentRepository.deleteById(comment.getId());
  }
}
