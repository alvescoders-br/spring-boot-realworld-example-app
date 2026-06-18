package io.spring.application.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
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
