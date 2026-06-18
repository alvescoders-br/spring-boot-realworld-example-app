package io.spring.application.data;

public record ArticleFavoriteCount(String id, Integer count) {
  public String getId() {
    return id;
  }

  public Integer getCount() {
    return count;
  }
}
