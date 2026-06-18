package io.spring.infrastructure.jpa.readservice;

import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager.Direction;
import io.spring.application.Page;
import io.spring.application.data.ArticleData;
import io.spring.infrastructure.jpa.repository.SpringDataJpaArticleRepository;
import io.spring.infrastructure.readservice.ArticleReadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaArticleReadService implements ArticleReadService {
  private static final String ARTICLE_DATA_SELECT =
      "select "
          + "A.id as articleId, A.slug as articleSlug, A.title as articleTitle, "
          + "A.description as articleDescription, A.body as articleBody, "
          + "extract(epoch from A.created_at) * 1000 as articleCreatedAt, "
          + "extract(epoch from A.updated_at) * 1000 as articleUpdatedAt, "
          + "A.reading_time as articleReadingTime, "
          + "T.name as tagName, U.id as userId, U.username as userUsername, "
          + "U.bio as userBio, U.image as userImage "
          + "from articles A "
          + "left join article_tags AT on A.id = AT.article_id "
          + "left join tags T on T.id = AT.tag_id "
          + "left join users U on U.id = A.user_id ";

  private static final String ARTICLE_FILTER_FROM =
      "from articles A "
          + "left join article_tags AT on A.id = AT.article_id "
          + "left join tags T on T.id = AT.tag_id "
          + "left join article_favorites AF on AF.article_id = A.id "
          + "left join users AU on AU.id = A.user_id "
          + "left join users AFU on AFU.id = AF.user_id ";

  private final EntityManager entityManager;
  private final SpringDataJpaArticleRepository articleRepository;

  public JpaArticleReadService(
      EntityManager entityManager, SpringDataJpaArticleRepository articleRepository) {
    this.entityManager = entityManager;
    this.articleRepository = articleRepository;
  }

  @Override
  @Transactional
  public ArticleData findById(String id) {
    List<ArticleData> articles =
        findArticleData(
            ARTICLE_DATA_SELECT + "where A.id = :id and A.is_deleted = false", Map.of("id", id));
    return articles.isEmpty() ? null : articles.get(0);
  }

  @Override
  @Transactional
  public ArticleData findBySlug(String slug) {
    List<ArticleData> articles =
        findArticleData(
            ARTICLE_DATA_SELECT + "where A.slug = :slug and A.is_deleted = false",
            Map.of("slug", slug));
    return articles.isEmpty() ? null : articles.get(0);
  }

  @Override
  public List<String> queryArticles(String tag, String author, String favoritedBy, Page page) {
    QueryParts queryParts = articleFilterWhere(tag, author, favoritedBy);
    Query query =
        entityManager.createNativeQuery(
            "select distinct A.id, A.created_at "
                + ARTICLE_FILTER_FROM
                + queryParts.whereClause()
                + " order by A.created_at desc limit :limit offset :offset");
    bindParameters(query, queryParts.parameters());
    query.setParameter("limit", page.getLimit());
    query.setParameter("offset", page.getOffset());
    return firstColumn(query.getResultList());
  }

  @Override
  public int countArticle(String tag, String author, String favoritedBy) {
    QueryParts queryParts = articleFilterWhere(tag, author, favoritedBy);
    Query query =
        entityManager.createNativeQuery(
            "select count(distinct A.id) " + ARTICLE_FILTER_FROM + queryParts.whereClause());
    bindParameters(query, queryParts.parameters());
    return ((Number) query.getSingleResult()).intValue();
  }

  @Override
  @Transactional
  public List<ArticleData> findArticles(List<String> articleIds) {
    if (articleIds == null || articleIds.isEmpty()) {
      return List.of();
    }
    return findArticleData(
        ARTICLE_DATA_SELECT
            + "where A.id in (:articleIds) and A.is_deleted = false order by A.created_at desc",
        Map.of("articleIds", articleIds));
  }

  @Override
  @Transactional
  public List<ArticleData> findArticlesOfAuthors(List<String> authors, Page page) {
    if (authors == null || authors.isEmpty()) {
      return List.of();
    }
    Query query =
        entityManager.createNativeQuery(
            ARTICLE_DATA_SELECT
                + "where A.user_id in (:authors) and A.is_deleted = false "
                + "order by A.created_at desc limit :limit offset :offset");
    query.setParameter("authors", authors);
    query.setParameter("limit", page.getLimit());
    query.setParameter("offset", page.getOffset());
    List<ArticleData> articles = JpaReadModelDataMapper.toArticleDataList(query.getResultList());
    cacheMissingReadingTimes(articles);
    return articles;
  }

  @Override
  @Transactional
  public List<ArticleData> findArticlesOfAuthorsWithCursor(
      List<String> authors, CursorPageParameter page) {
    if (authors == null || authors.isEmpty()) {
      return List.of();
    }
    StringBuilder sql =
        new StringBuilder(
            ARTICLE_DATA_SELECT + "where A.user_id in (:authors) and A.is_deleted = false ");
    if (page.getCursor() != null && page.getDirection() == Direction.NEXT) {
      sql.append("and A.created_at < :cursor ");
    }
    if (page.getCursor() != null && page.getDirection() == Direction.PREV) {
      sql.append("and A.created_at > :cursor ");
    }
    sql.append(
        page.getDirection() == Direction.PREV
            ? "order by A.created_at asc "
            : "order by A.created_at desc ");
    sql.append("limit :limit");
    Query query = entityManager.createNativeQuery(sql.toString());
    query.setParameter("authors", authors);
    if (page.getCursor() != null) {
      query.setParameter("cursor", page.getCursor());
    }
    query.setParameter("limit", page.getQueryLimit());
    List<ArticleData> articles = JpaReadModelDataMapper.toArticleDataList(query.getResultList());
    cacheMissingReadingTimes(articles);
    return articles;
  }

  @Override
  public int countFeedSize(List<String> authors) {
    if (authors == null || authors.isEmpty()) {
      return 0;
    }
    Query query =
        entityManager.createNativeQuery(
            "select count(1) from articles A where A.user_id in (:authors) and A.is_deleted = false");
    query.setParameter("authors", authors);
    return ((Number) query.getSingleResult()).intValue();
  }

  @Override
  public List<String> findArticlesWithCursor(
      String tag, String author, String favoritedBy, CursorPageParameter page) {
    QueryParts queryParts = articleFilterWhere(tag, author, favoritedBy);
    StringBuilder sql =
        new StringBuilder(
            "select distinct A.id, A.created_at " + ARTICLE_FILTER_FROM + queryParts.whereClause());
    if (page.getCursor() != null) {
      sql.append(queryParts.hasWhereClause() ? " and " : " where ");
      sql.append(
          page.getDirection() == Direction.PREV
              ? "A.created_at > :cursor"
              : "A.created_at < :cursor");
    }
    sql.append(
        page.getDirection() == Direction.PREV
            ? " order by A.created_at asc"
            : " order by A.created_at desc");
    sql.append(" limit :limit");
    Query query = entityManager.createNativeQuery(sql.toString());
    bindParameters(query, queryParts.parameters());
    if (page.getCursor() != null) {
      query.setParameter("cursor", page.getCursor());
    }
    query.setParameter("limit", page.getQueryLimit());
    return firstColumn(query.getResultList());
  }

  private List<ArticleData> findArticleData(String sql, Map<String, Object> parameters) {
    Query query = entityManager.createNativeQuery(sql);
    bindParameters(query, parameters);
    List<ArticleData> articles = JpaReadModelDataMapper.toArticleDataList(query.getResultList());
    cacheMissingReadingTimes(articles);
    return articles;
  }

  private void cacheMissingReadingTimes(List<ArticleData> articles) {
    for (ArticleData article : articles) {
      if (article.getCachedReadingTime() != null) {
        continue;
      }
      int readingTime = article.getReadingTime();
      articleRepository.cacheReadingTimeIfAbsent(article.getId(), readingTime);
      article.setCachedReadingTime(readingTime);
    }
  }

  private QueryParts articleFilterWhere(String tag, String author, String favoritedBy) {
    List<String> conditions = new ArrayList<>();
    java.util.LinkedHashMap<String, Object> parameters = new java.util.LinkedHashMap<>();
    conditions.add("A.is_deleted = false");
    if (tag != null) {
      conditions.add("T.name = :tag");
      parameters.put("tag", tag);
    }
    if (author != null) {
      conditions.add("AU.username = :author");
      parameters.put("author", author);
    }
    if (favoritedBy != null) {
      conditions.add("AFU.username = :favoritedBy");
      parameters.put("favoritedBy", favoritedBy);
    }
    return new QueryParts(" where " + String.join(" and ", conditions), parameters);
  }

  private void bindParameters(Query query, Map<String, Object> parameters) {
    parameters.forEach(query::setParameter);
  }

  private List<String> firstColumn(List<Object[]> rows) {
    return rows.stream().map(row -> JpaReadModelDataMapper.stringValue(row[0])).toList();
  }

  private static class QueryParts {
    private final String whereClause;
    private final Map<String, Object> parameters;

    QueryParts(String whereClause, Map<String, Object> parameters) {
      this.whereClause = whereClause;
      this.parameters = parameters;
    }

    String whereClause() {
      return whereClause;
    }

    Map<String, Object> parameters() {
      return parameters;
    }

    boolean hasWhereClause() {
      return !whereClause.isBlank();
    }
  }
}
