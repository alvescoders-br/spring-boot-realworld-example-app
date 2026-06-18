package io.spring.infrastructure.readservice;

import io.spring.application.CursorPageParameter;
import io.spring.application.Page;
import io.spring.application.data.ArticleData;
import java.util.List;

public interface ArticleReadService {
  ArticleData findById(String id);

  ArticleData findBySlug(String slug);

  List<String> queryArticles(String tag, String author, String favoritedBy, Page page);

  int countArticle(String tag, String author, String favoritedBy);

  List<ArticleData> findArticles(List<String> articleIds);

  List<ArticleData> findArticlesOfAuthors(List<String> authors, Page page);

  List<ArticleData> findArticlesOfAuthorsWithCursor(List<String> authors, CursorPageParameter page);

  int countFeedSize(List<String> authors);

  List<String> findArticlesWithCursor(
      String tag, String author, String favoritedBy, CursorPageParameter page);
}
