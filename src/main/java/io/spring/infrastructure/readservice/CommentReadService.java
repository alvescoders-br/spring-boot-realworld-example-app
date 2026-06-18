package io.spring.infrastructure.readservice;

import io.spring.application.CursorPageParameter;
import io.spring.application.data.CommentData;
import java.time.Instant;
import java.util.List;

public interface CommentReadService {
  CommentData findById(String id);

  List<CommentData> findByArticleId(String articleId);

  List<CommentData> findByArticleIdWithCursor(
      String articleId, CursorPageParameter<Instant> page);
}
