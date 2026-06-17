package io.spring.infrastructure.jpa.readservice;

import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JpaReadModelDataMapper {
  private JpaReadModelDataMapper() {}

  static List<ArticleData> toArticleDataList(List<Object[]> rows) {
    Map<String, ArticleData> articlesById = new LinkedHashMap<>();
    for (Object[] row : rows) {
      String articleId = stringValue(row[0]);
      ArticleData articleData = articlesById.computeIfAbsent(articleId, ignored -> toArticleData(row));
      String tagName = stringValue(row[7]);
      if (tagName != null && !articleData.getTagList().contains(tagName)) {
        articleData.getTagList().add(tagName);
      }
    }
    return new ArrayList<>(articlesById.values());
  }

  static ArticleData toArticleData(Object[] row) {
    return new ArticleData(
        stringValue(row[0]),
        stringValue(row[1]),
        stringValue(row[2]),
        stringValue(row[3]),
        stringValue(row[4]),
        false,
        0,
        instantValue(row[5]),
        instantValue(row[6]),
        new ArrayList<>(),
        new ProfileData(
            stringValue(row[8]), stringValue(row[9]), stringValue(row[10]), stringValue(row[11]), false));
  }

  static CommentData toCommentData(Object[] row) {
    Instant createdAt = instantValue(row[2]);
    return new CommentData(
        stringValue(row[0]),
        stringValue(row[1]),
        stringValue(row[3]),
        createdAt,
        createdAt,
        new ProfileData(
            stringValue(row[4]), stringValue(row[5]), stringValue(row[6]), stringValue(row[7]), false));
  }

  static Instant instantValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return Instant.ofEpochMilli(number.longValue());
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.toInstant(ZoneOffset.UTC);
    }
    return Instant.parse(value.toString());
  }

  static String stringValue(Object value) {
    return value == null ? null : value.toString();
  }
}
