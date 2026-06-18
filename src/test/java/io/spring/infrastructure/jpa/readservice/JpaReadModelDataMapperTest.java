package io.spring.infrastructure.jpa.readservice;

import static org.assertj.core.api.Assertions.assertThat;

import io.spring.application.data.ArticleData;
import io.spring.application.data.CommentData;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class JpaReadModelDataMapperTest {

  @Test
  void shouldAggregateArticleRowsAndDeduplicateTags() {
    Instant createdAt = Instant.parse("2026-06-17T12:00:00Z");
    Object[] firstRow = articleRow("java", createdAt.toEpochMilli(), Timestamp.from(createdAt));
    Object[] duplicateTagRow = articleRow("java", createdAt.toEpochMilli(), Timestamp.from(createdAt));
    Object[] secondTagRow = articleRow("spring", createdAt.toEpochMilli(), Timestamp.from(createdAt));

    List<ArticleData> articles =
        JpaReadModelDataMapper.toArticleDataList(List.of(firstRow, duplicateTagRow, secondTagRow));

    assertThat(articles).hasSize(1);
    ArticleData article = articles.get(0);
    assertThat(article.getSlug()).isEqualTo("article-slug");
    assertThat(article.getBody()).isEqualTo("article body");
    assertThat(article.getCreatedAt()).isEqualTo(createdAt);
    assertThat(article.getUpdatedAt()).isNotNull();
    assertThat(article.getTagList()).containsExactly("java", "spring");
    assertThat(article.getProfileData().getUsername()).isEqualTo("author");
  }

  @Test
  void shouldMapCommentRowWithLocalDateTimeCreatedAt() {
    LocalDateTime createdAt = LocalDateTime.parse("2026-06-17T12:10:00");

    CommentData comment =
        JpaReadModelDataMapper.toCommentData(
            new Object[] {
              "comment-id",
              "comment body",
              createdAt,
              "article-id",
              "user-id",
              "author",
              "bio",
              "image"
            });

    assertThat(comment.getId()).isEqualTo("comment-id");
    assertThat(comment.getBody()).isEqualTo("comment body");
    assertThat(comment.getCreatedAt()).isEqualTo(Instant.parse("2026-06-17T12:10:00Z"));
    assertThat(comment.getUpdatedAt()).isEqualTo(Instant.parse("2026-06-17T12:10:00Z"));
    assertThat(comment.getProfileData().getUsername()).isEqualTo("author");
  }

  @Test
  void shouldParseStringInstantsAndNullValues() {
    assertThat(JpaReadModelDataMapper.instantValue(null)).isNull();
    assertThat(JpaReadModelDataMapper.stringValue(null)).isNull();
    assertThat(JpaReadModelDataMapper.instantValue("2026-06-17T12:00:00Z"))
        .isEqualTo(Instant.parse("2026-06-17T12:00:00Z"));
  }

  private Object[] articleRow(String tagName, Object createdAt, Object updatedAt) {
    return new Object[] {
      "article-id",
      "article-slug",
      "Article title",
      "description",
      "article body",
      createdAt,
      updatedAt,
      tagName,
      "user-id",
      "author",
      "bio",
      "image"
    };
  }
}
