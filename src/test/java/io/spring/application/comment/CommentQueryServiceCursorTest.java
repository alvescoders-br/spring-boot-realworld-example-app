package io.spring.application.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager;
import io.spring.application.CursorPager.Direction;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.user.User;
import io.spring.infrastructure.readservice.CommentReadService;
import io.spring.infrastructure.readservice.UserRelationshipQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceCursorTest {
  @Mock private CommentReadService commentReadService;
  @Mock private UserRelationshipQueryService userRelationshipQueryService;

  @Test
  void should_return_empty_cursor_page_when_no_comments_exist() {
    CommentQueryService service =
        new CommentQueryService(commentReadService, userRelationshipQueryService);
    CursorPageParameter<Instant> page = new CursorPageParameter<>(null, 20, Direction.NEXT);
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    when(commentReadService.findByArticleIdWithCursor(eq("article-id"), eq(page)))
        .thenReturn(List.of());

    CursorPager<CommentData> result =
        service.findByArticleIdWithCursor("article-id", currentUser, page);

    assertTrue(result.getData().isEmpty());
    assertFalse(result.hasNext());
    assertFalse(result.hasPrevious());
    assertNull(result.getStartCursor());
    assertNull(result.getEndCursor());
  }

  @Test
  void should_trim_extra_comment_and_mark_followed_author_on_next_page() {
    CommentQueryService service =
        new CommentQueryService(commentReadService, userRelationshipQueryService);
    CursorPageParameter<Instant> page = new CursorPageParameter<>(null, 1, Direction.NEXT);
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    CommentData first = comment("comment-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    CommentData extra = comment("comment-2", "author-2", Instant.parse("2026-06-16T09:00:00Z"));
    when(commentReadService.findByArticleIdWithCursor(eq("article-id"), eq(page)))
        .thenReturn(new java.util.ArrayList<>(List.of(first, extra)));
    when(userRelationshipQueryService.followingAuthors(
            eq(currentUser.getId()), eq(List.of("author-1", "author-2"))))
        .thenReturn(Set.of("author-1"));

    CursorPager<CommentData> result =
        service.findByArticleIdWithCursor("article-id", currentUser, page);

    assertEquals(1, result.getData().size());
    assertEquals("comment-1", result.getData().get(0).getId());
    assertTrue(result.getData().get(0).getProfileData().isFollowing());
    assertTrue(result.hasNext());
    assertFalse(result.hasPrevious());
    assertEquals("1781604000000", result.getEndCursor().toString());
  }

  @Test
  void should_reverse_previous_page_after_trimming_extra_comment() {
    CommentQueryService service =
        new CommentQueryService(commentReadService, userRelationshipQueryService);
    CursorPageParameter<Instant> page = new CursorPageParameter<>(null, 2, Direction.PREV);
    CommentData first = comment("comment-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    CommentData second = comment("comment-2", "author-2", Instant.parse("2026-06-16T09:00:00Z"));
    CommentData extra = comment("comment-3", "author-3", Instant.parse("2026-06-16T08:00:00Z"));
    when(commentReadService.findByArticleIdWithCursor(eq("article-id"), eq(page)))
        .thenReturn(new java.util.ArrayList<>(List.of(first, second, extra)));

    CursorPager<CommentData> result = service.findByArticleIdWithCursor("article-id", null, page);

    assertEquals(2, result.getData().size());
    assertEquals("comment-2", result.getData().get(0).getId());
    assertEquals("comment-1", result.getData().get(1).getId());
    assertFalse(result.hasNext());
    assertTrue(result.hasPrevious());
    assertFalse(result.getData().get(0).getProfileData().isFollowing());
  }

  @Test
  void should_mark_followed_authors_for_non_cursor_comments() {
    CommentQueryService service =
        new CommentQueryService(commentReadService, userRelationshipQueryService);
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    CommentData first = comment("comment-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    CommentData second = comment("comment-2", "author-2", Instant.parse("2026-06-16T09:00:00Z"));
    when(commentReadService.findByArticleId(eq("article-id"))).thenReturn(List.of(first, second));
    when(userRelationshipQueryService.followingAuthors(
            eq(currentUser.getId()), eq(List.of("author-1", "author-2"))))
        .thenReturn(Set.of("author-2"));

    List<CommentData> comments = service.findByArticleId("article-id", currentUser);

    assertEquals(2, comments.size());
    assertFalse(comments.get(0).getProfileData().isFollowing());
    assertTrue(comments.get(1).getProfileData().isFollowing());
  }

  @Test
  void should_not_mark_following_authors_when_non_cursor_user_is_absent() {
    CommentQueryService service =
        new CommentQueryService(commentReadService, userRelationshipQueryService);
    CommentData first = comment("comment-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    when(commentReadService.findByArticleId(eq("article-id"))).thenReturn(List.of(first));

    List<CommentData> comments = service.findByArticleId("article-id", null);

    assertEquals(1, comments.size());
    assertFalse(comments.get(0).getProfileData().isFollowing());
    verifyNoInteractions(userRelationshipQueryService);
  }

  @Test
  void should_mark_following_author_when_finding_comment_by_id_for_current_user() {
    CommentQueryService service =
        new CommentQueryService(commentReadService, userRelationshipQueryService);
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    CommentData first = comment("comment-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    when(commentReadService.findById(eq("comment-1"))).thenReturn(first);
    when(userRelationshipQueryService.isUserFollowing(eq(currentUser.getId()), eq("author-1")))
        .thenReturn(true);

    CommentData comment = service.findById("comment-1", currentUser).orElseThrow();

    assertTrue(comment.getProfileData().isFollowing());
  }

  @Test
  void should_not_query_following_authors_when_non_cursor_comments_are_empty() {
    CommentQueryService service =
        new CommentQueryService(commentReadService, userRelationshipQueryService);
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    when(commentReadService.findByArticleId(eq("article-id"))).thenReturn(List.of());

    List<CommentData> comments = service.findByArticleId("article-id", currentUser);

    assertTrue(comments.isEmpty());
    verifyNoInteractions(userRelationshipQueryService);
  }

  @Test
  void should_not_mark_extra_page_when_cursor_comments_match_limit_exactly() {
    CommentQueryService service =
        new CommentQueryService(commentReadService, userRelationshipQueryService);
    CursorPageParameter<Instant> page = new CursorPageParameter<>(null, 2, Direction.NEXT);
    CommentData first = comment("comment-1", "author-1", Instant.parse("2026-06-16T10:00:00Z"));
    CommentData second = comment("comment-2", "author-2", Instant.parse("2026-06-16T09:00:00Z"));
    when(commentReadService.findByArticleIdWithCursor(eq("article-id"), eq(page)))
        .thenReturn(new java.util.ArrayList<>(List.of(first, second)));

    CursorPager<CommentData> result = service.findByArticleIdWithCursor("article-id", null, page);

    assertEquals(2, result.getData().size());
    assertFalse(result.hasNext());
    assertFalse(result.hasPrevious());
  }

  private CommentData comment(String id, String authorId, Instant createdAt) {
    return new CommentData(
        id,
        "comment body",
        "article-id",
        createdAt,
        createdAt,
        new ProfileData(authorId, authorId, "", "", false));
  }
}
