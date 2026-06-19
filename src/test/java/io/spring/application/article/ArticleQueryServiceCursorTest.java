package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.spring.application.ArticleQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleFavoriteCount;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.infrastructure.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.readservice.ArticleReadService;
import io.spring.infrastructure.readservice.UserRelationshipQueryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleQueryServiceCursorTest {
  @Mock private ArticleReadService articleReadService;
  @Mock private UserRelationshipQueryService userRelationshipQueryService;
  @Mock private ArticleFavoritesReadService articleFavoritesReadService;

  @Test
  void should_return_empty_user_feed_cursor_when_user_follows_no_authors() {
    ArticleQueryService service = articleQueryService();
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    CursorPageParameter<Instant> page =
        new CursorPageParameter<>(Instant.parse("2026-06-16T10:00:00Z"), 20, Direction.NEXT);
    when(userRelationshipQueryService.followedUsers(eq(currentUser.getId()))).thenReturn(List.of());

    CursorPager<ArticleData> result = service.findUserFeedWithCursor(currentUser, page);

    assertTrue(result.getData().isEmpty());
    assertFalse(result.hasNext());
    assertTrue(result.hasPrevious());
  }

  @Test
  void should_trim_reverse_and_fill_extra_info_for_previous_user_feed_cursor() {
    ArticleQueryService service = articleQueryService();
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    CursorPageParameter<Instant> page =
        new CursorPageParameter<>(Instant.parse("2026-06-16T11:00:00Z"), 2, Direction.PREV);
    ArticleData first = article("article-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    ArticleData second = article("article-2", "author-2", Instant.parse("2026-06-16T09:00:00Z"));
    ArticleData extra = article("article-3", "author-3", Instant.parse("2026-06-16T08:00:00Z"));
    when(userRelationshipQueryService.followedUsers(eq(currentUser.getId())))
        .thenReturn(List.of("author-1", "author-2"));
    when(articleReadService.findArticlesOfAuthorsWithCursor(
            eq(List.of("author-1", "author-2")), eq(page)))
        .thenReturn(new ArrayList<>(List.of(first, second, extra)));
    when(articleFavoritesReadService.articlesFavoriteCount(eq(List.of("article-2", "article-1"))))
        .thenReturn(
            List.of(
                new ArticleFavoriteCount("article-2", 3),
                new ArticleFavoriteCount("article-1", 0)));
    when(articleFavoritesReadService.userFavorites(eq(List.of("article-2", "article-1")), eq(currentUser)))
        .thenReturn(Set.of("article-1"));
    when(userRelationshipQueryService.followingAuthors(
            eq(currentUser.getId()), eq(List.of("author-2", "author-1"))))
        .thenReturn(Set.of("author-2"));

    CursorPager<ArticleData> result = service.findUserFeedWithCursor(currentUser, page);

    assertEquals(2, result.getData().size());
    assertEquals("article-2", result.getData().get(0).getId());
    assertEquals("article-1", result.getData().get(1).getId());
    assertEquals(3, result.getData().get(0).getFavoritesCount());
    assertEquals(0, result.getData().get(1).getFavoritesCount());
    assertFalse(result.getData().get(0).isFavorited());
    assertTrue(result.getData().get(1).isFavorited());
    assertTrue(result.getData().get(0).getProfileData().isFollowing());
    assertFalse(result.getData().get(1).getProfileData().isFollowing());
    assertTrue(result.hasNext());
    assertTrue(result.hasPrevious());
  }

  @Test
  void should_return_empty_recent_articles_cursor_with_existing_cursor() {
    ArticleQueryService service = articleQueryService();
    CursorPageParameter<Instant> page =
        new CursorPageParameter<>(Instant.parse("2026-06-16T11:00:00Z"), 20, Direction.NEXT);
    when(articleReadService.findArticlesWithCursor(eq("java"), eq(null), eq(null), eq(page)))
        .thenReturn(List.of());

    CursorPager<ArticleData> result =
        service.findRecentArticlesWithCursor("java", null, null, page, null);

    assertTrue(result.getData().isEmpty());
    assertFalse(result.hasNext());
    assertTrue(result.hasPrevious());
  }

  @Test
  void should_trim_reverse_and_fill_extra_info_for_previous_recent_articles_cursor() {
    ArticleQueryService service = articleQueryService();
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    CursorPageParameter<Instant> page =
        new CursorPageParameter<>(Instant.parse("2026-06-16T11:00:00Z"), 2, Direction.PREV);
    ArticleData first = article("article-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    ArticleData second = article("article-2", "author-2", Instant.parse("2026-06-16T09:00:00Z"));
    when(articleReadService.findArticlesWithCursor(eq(null), eq(null), eq(null), eq(page)))
        .thenReturn(new ArrayList<>(List.of("article-1", "article-2", "article-3")));
    when(articleReadService.findArticles(eq(List.of("article-2", "article-1"))))
        .thenReturn(List.of(second, first));
    when(articleFavoritesReadService.articlesFavoriteCount(eq(List.of("article-2", "article-1"))))
        .thenReturn(
            List.of(
                new ArticleFavoriteCount("article-2", 2),
                new ArticleFavoriteCount("article-1", 1)));
    when(articleFavoritesReadService.userFavorites(eq(List.of("article-2", "article-1")), eq(currentUser)))
        .thenReturn(Set.of("article-2"));
    when(userRelationshipQueryService.followingAuthors(
            eq(currentUser.getId()), eq(List.of("author-2", "author-1"))))
        .thenReturn(Set.of("author-1"));

    CursorPager<ArticleData> result =
        service.findRecentArticlesWithCursor(null, null, null, page, currentUser);

    assertEquals(2, result.getData().size());
    assertEquals("article-2", result.getData().get(0).getId());
    assertEquals("article-1", result.getData().get(1).getId());
    assertEquals(2, result.getData().get(0).getFavoritesCount());
    assertEquals(1, result.getData().get(1).getFavoritesCount());
    assertTrue(result.getData().get(0).isFavorited());
    assertFalse(result.getData().get(1).isFavorited());
    assertFalse(result.getData().get(0).getProfileData().isFollowing());
    assertTrue(result.getData().get(1).getProfileData().isFollowing());
    assertTrue(result.hasNext());
    assertTrue(result.hasPrevious());
    assertEquals("1781600400000", result.getStartCursor().toString());
  }

  @Test
  void should_mark_following_author_when_finding_article_by_id_for_current_user() {
    ArticleQueryService service = articleQueryService();
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    ArticleData article = article("article-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    when(articleReadService.findById(eq("article-1"))).thenReturn(article);
    when(articleFavoritesReadService.isUserFavorite(eq(currentUser.getId()), eq("article-1")))
        .thenReturn(true);
    when(articleFavoritesReadService.articleFavoriteCount(eq("article-1"))).thenReturn(7);
    when(userRelationshipQueryService.isUserFollowing(eq(currentUser.getId()), eq("author-1")))
        .thenReturn(true);

    ArticleData result = service.findById("article-1", currentUser).orElseThrow();

    assertTrue(result.isFavorited());
    assertEquals(7, result.getFavoritesCount());
    assertTrue(result.getProfileData().isFollowing());
  }

  @Test
  void should_not_mark_extra_page_when_recent_articles_match_limit_exactly() {
    ArticleQueryService service = articleQueryService();
    CursorPageParameter<Instant> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    ArticleData first = article("article-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    ArticleData second = article("article-2", "author-2", Instant.parse("2026-06-16T09:00:00Z"));
    when(articleReadService.findArticlesWithCursor(eq(null), eq(null), eq(null), eq(page)))
        .thenReturn(new ArrayList<>(List.of("article-1", "article-2")));
    when(articleReadService.findArticles(eq(List.of("article-1", "article-2"))))
        .thenReturn(List.of(first, second));
    when(articleFavoritesReadService.articlesFavoriteCount(eq(List.of("article-1", "article-2"))))
        .thenReturn(
            List.of(
                new ArticleFavoriteCount("article-1", 0),
                new ArticleFavoriteCount("article-2", 0)));

    CursorPager<ArticleData> result =
        service.findRecentArticlesWithCursor(null, null, null, page, null);

    assertEquals(2, result.getData().size());
    assertFalse(result.hasNext());
    assertFalse(result.hasPrevious());
  }

  @Test
  void should_not_mark_extra_page_when_user_feed_matches_limit_exactly() {
    ArticleQueryService service = articleQueryService();
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    CursorPageParameter<Instant> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    ArticleData first = article("article-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    ArticleData second = article("article-2", "author-2", Instant.parse("2026-06-16T09:00:00Z"));
    when(userRelationshipQueryService.followedUsers(eq(currentUser.getId())))
        .thenReturn(List.of("author-1", "author-2"));
    when(articleReadService.findArticlesOfAuthorsWithCursor(
            eq(List.of("author-1", "author-2")), eq(page)))
        .thenReturn(new ArrayList<>(List.of(first, second)));
    when(articleFavoritesReadService.articlesFavoriteCount(eq(List.of("article-1", "article-2"))))
        .thenReturn(
            List.of(
                new ArticleFavoriteCount("article-1", 0),
                new ArticleFavoriteCount("article-2", 0)));
    when(articleFavoritesReadService.userFavorites(eq(List.of("article-1", "article-2")), eq(currentUser)))
        .thenReturn(Set.of());
    when(userRelationshipQueryService.followingAuthors(
            eq(currentUser.getId()), eq(List.of("author-1", "author-2"))))
        .thenReturn(Set.of());

    CursorPager<ArticleData> result = service.findUserFeedWithCursor(currentUser, page);

    assertEquals(2, result.getData().size());
    assertFalse(result.hasNext());
    assertFalse(result.hasPrevious());
  }

  private ArticleQueryService articleQueryService() {
    return new ArticleQueryService(
        articleReadService, userRelationshipQueryService, articleFavoritesReadService);
  }

  private ArticleData article(String id, String authorId, Instant updatedAt) {
    return new ArticleData(
        id,
        id + "-slug",
        id + " title",
        "description",
        "body",
        false,
        0,
        updatedAt,
        updatedAt,
        List.of("java"),
        new ProfileData(authorId, authorId, "", "", false));
  }
}
