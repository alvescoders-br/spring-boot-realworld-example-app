package io.spring.infrastructure.jpa.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JpaCompositeIdTest {

  @Test
  void shouldCompareArticleFavoriteIdsByArticleAndUser() {
    JpaArticleFavoriteId id = new JpaArticleFavoriteId("article-id", "user-id");
    JpaArticleFavoriteId same = new JpaArticleFavoriteId("article-id", "user-id");
    JpaArticleFavoriteId different = new JpaArticleFavoriteId("article-id", "other-user-id");

    assertThat(id.getArticleId()).isEqualTo("article-id");
    assertThat(id.getUserId()).isEqualTo("user-id");
    assertThat(id).isEqualTo(id);
    assertThat(id).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(id).isNotEqualTo(different).isNotEqualTo("article-id");
  }

  @Test
  void shouldCompareArticleTagRelationIdsByArticleAndTag() {
    JpaArticleTagRelationId id = new JpaArticleTagRelationId("article-id", "tag-id");
    JpaArticleTagRelationId same = new JpaArticleTagRelationId("article-id", "tag-id");
    JpaArticleTagRelationId different = new JpaArticleTagRelationId("article-id", "other-tag-id");

    assertThat(id.getArticleId()).isEqualTo("article-id");
    assertThat(id.getTagId()).isEqualTo("tag-id");
    assertThat(id).isEqualTo(id);
    assertThat(id).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(id).isNotEqualTo(different).isNotEqualTo("tag-id");
  }

  @Test
  void shouldCompareFollowRelationIdsByUserAndTarget() {
    JpaFollowRelationId id = new JpaFollowRelationId("user-id", "target-id");
    JpaFollowRelationId same = new JpaFollowRelationId("user-id", "target-id");
    JpaFollowRelationId different = new JpaFollowRelationId("user-id", "other-target-id");

    assertThat(id.getUserId()).isEqualTo("user-id");
    assertThat(id.getTargetId()).isEqualTo("target-id");
    assertThat(id).isEqualTo(id);
    assertThat(id).isEqualTo(same).hasSameHashCodeAs(same);
    assertThat(id).isNotEqualTo(different).isNotEqualTo("target-id");
  }
}
