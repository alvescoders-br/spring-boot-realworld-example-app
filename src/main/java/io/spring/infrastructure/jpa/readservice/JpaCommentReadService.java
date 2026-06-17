package io.spring.infrastructure.jpa.readservice;

import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.infrastructure.mybatis.readservice.CommentReadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Primary
@Profile("postgres")
@Transactional(readOnly = true)
public class JpaCommentReadService implements CommentReadService {
  private static final String COMMENT_DATA_SELECT =
      "select C.id as commentId, C.body as commentBody, "
          + "extract(epoch from C.created_at) * 1000 as commentCreatedAt, "
          + "C.article_id as commentArticleId, U.id as userId, U.username as userUsername, "
          + "U.bio as userBio, U.image as userImage "
          + "from comments C left join users U on C.user_id = U.id ";

  private final EntityManager entityManager;

  public JpaCommentReadService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public CommentData findById(String id) {
    Query query = entityManager.createNativeQuery(COMMENT_DATA_SELECT + "where C.id = :id");
    query.setParameter("id", id);
    List<CommentData> comments = toCommentDataList(query.getResultList());
    return comments.isEmpty() ? null : comments.get(0);
  }

  @Override
  public List<CommentData> findByArticleId(String articleId) {
    Query query =
        entityManager.createNativeQuery(COMMENT_DATA_SELECT + "where C.article_id = :articleId");
    query.setParameter("articleId", articleId);
    return toCommentDataList(query.getResultList());
  }

  @Override
  public List<CommentData> findByArticleIdWithCursor(
      String articleId, CursorPageParameter<Instant> page) {
    StringBuilder sql = new StringBuilder(COMMENT_DATA_SELECT + "where C.article_id = :articleId ");
    if (page.getCursor() != null && page.getDirection() == Direction.NEXT) {
      sql.append("and C.created_at < :cursor ");
    }
    if (page.getCursor() != null && page.getDirection() == Direction.PREV) {
      sql.append("and C.created_at > :cursor ");
    }
    sql.append(page.getDirection() == Direction.PREV ? "order by C.created_at asc" : "order by C.created_at desc");
    Query query = entityManager.createNativeQuery(sql.toString());
    query.setParameter("articleId", articleId);
    if (page.getCursor() != null) {
      query.setParameter("cursor", page.getCursor());
    }
    return toCommentDataList(query.getResultList());
  }

  private List<CommentData> toCommentDataList(List<Object[]> rows) {
    return rows.stream().map(JpaReadModelDataMapper::toCommentData).toList();
  }
}
