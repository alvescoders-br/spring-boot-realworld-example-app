package io.spring.application.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.spring.application.DateTimeCursor;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ArticleData implements io.spring.application.Node {
  private String id;
  private String slug;
  private String title;
  private String description;
  private String body;
  private boolean favorited;
  private int favoritesCount;
  private Instant createdAt;
  private Instant updatedAt;
  private List<String> tagList;

  @JsonProperty("author")
  private ProfileData profileData;

  @JsonIgnore private Integer cachedReadingTime;

  public ArticleData(
      String id,
      String slug,
      String title,
      String description,
      String body,
      boolean favorited,
      int favoritesCount,
      Instant createdAt,
      Instant updatedAt,
      List<String> tagList,
      ProfileData profileData) {
    this.id = id;
    this.slug = slug;
    this.title = title;
    this.description = description;
    this.body = body;
    this.favorited = favorited;
    this.favoritesCount = favoritesCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.tagList = tagList;
    this.profileData = profileData;
  }

  public int getReadingTime() {
    if (cachedReadingTime != null) {
      return cachedReadingTime;
    }
    return calculateReadingTime(body);
  }

  public static int calculateReadingTime(String body) {
    if (body == null || body.isBlank()) {
      return 0;
    }

    int wordCount = body.trim().split("\\s+").length;
    return (wordCount + 199) / 200;
  }

  @Override
  public DateTimeCursor getCursor() {
    return new DateTimeCursor(updatedAt);
  }
}
