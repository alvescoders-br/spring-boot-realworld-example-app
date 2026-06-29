package io.spring.core.article;

import static java.util.stream.Collectors.toList;

import io.spring.DateTimes;
import io.spring.Util;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Article {
  private String userId;
  private String id;
  private String slug;
  private String title;
  private String description;
  private String body;
  private List<Tag> tags;
  private Instant createdAt;
  private Instant updatedAt;

  public Article(
      String title, String description, String body, List<String> tagList, String userId) {
    this(title, description, body, tagList, userId, DateTimes.now());
  }

  public Article(
      String title,
      String description,
      String body,
      List<String> tagList,
      String userId,
      Instant createdAt) {
    this.id = UUID.randomUUID().toString();
    this.slug = toSlug(title);
    this.title = title;
    this.description = description;
    this.body = body;
    List<String> safeTagList = tagList == null ? List.of() : tagList;
    this.tags = new HashSet<>(safeTagList).stream().map(Tag::new).collect(toList());
    this.userId = userId;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public void update(String title, String description, String body) {
    if (!Util.isEmpty(title)) {
      this.title = title;
      this.slug = toSlug(title);
      this.updatedAt = DateTimes.now();
    }
    if (!Util.isEmpty(description)) {
      this.description = description;
      this.updatedAt = DateTimes.now();
    }
    if (!Util.isEmpty(body)) {
      this.body = body;
      this.updatedAt = DateTimes.now();
    }
  }

  public static Article restored(
      String id,
      String userId,
      String slug,
      String title,
      String description,
      String body,
      List<Tag> tags,
      Instant createdAt,
      Instant updatedAt) {
    Article article = new Article();
    article.id = id;
    article.userId = userId;
    article.slug = slug;
    article.title = title;
    article.description = description;
    article.body = body;
    article.tags = tags;
    article.createdAt = createdAt;
    article.updatedAt = updatedAt;
    return article;
  }

  public static String toSlug(String title) {
    return title.toLowerCase().replaceAll("[\\&|[\\uFE30-\\uFFA0]|\\’|\\”|\\s\\?\\,\\.]+", "-");
  }
}
