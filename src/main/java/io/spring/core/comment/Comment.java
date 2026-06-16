package io.spring.core.comment;

import io.spring.DateTimes;
import java.time.Instant;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Comment {
  private String id;
  private String body;
  private String userId;
  private String articleId;
  private Instant createdAt;

  public Comment(String body, String userId, String articleId) {
    this.id = UUID.randomUUID().toString();
    this.body = body;
    this.userId = userId;
    this.articleId = articleId;
    this.createdAt = DateTimes.now();
  }

  public static Comment restored(
      String id, String body, String userId, String articleId, Instant createdAt) {
    Comment comment = new Comment();
    comment.id = id;
    comment.body = body;
    comment.userId = userId;
    comment.articleId = articleId;
    comment.createdAt = createdAt;
    return comment;
  }
}
