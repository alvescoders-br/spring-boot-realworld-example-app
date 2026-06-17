package io.spring.infrastructure.jpa.readservice;

import io.spring.application.ArticleQueryService;
import io.spring.application.CommentQueryService;
import io.spring.application.CursorPageParameter;
import io.spring.application.CursorPager.Direction;
import io.spring.application.Page;
import io.spring.application.ProfileQueryService;
import io.spring.application.TagsQueryService;
import io.spring.application.data.ArticleData;
import io.spring.application.data.ArticleDataList;
import io.spring.application.data.CommentData;
import io.spring.application.data.ProfileData;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.mybatis.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.mybatis.readservice.ArticleReadService;
import io.spring.infrastructure.mybatis.readservice.CommentReadService;
import io.spring.infrastructure.mybatis.readservice.TagReadService;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("postgres")
@Transactional
@EnabledIfEnvironmentVariable(named = "REALWORLD_POSTGRES_URL", matches = "jdbc:postgresql:.*")
public class JpaReadServicePostgresIntegrationTest {
  @Autowired private ArticleQueryService articleQueryService;
  @Autowired private CommentQueryService commentQueryService;
  @Autowired private ProfileQueryService profileQueryService;
  @Autowired private TagsQueryService tagsQueryService;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleFavoriteRepository articleFavoriteRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private ArticleReadService articleReadService;
  @Autowired private ArticleFavoritesReadService articleFavoritesReadService;
  @Autowired private UserRelationshipQueryService userRelationshipQueryService;
  @Autowired private UserReadService userReadService;
  @Autowired private TagReadService tagReadService;
  @Autowired private CommentReadService commentReadService;

  @Test
  public void should_use_jpa_read_services_and_preserve_rest_read_model_equivalence() {
    assertJpaReadServicesArePrimaryBeans();
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    User author = user("author-" + suffix);
    User secondAuthor = user("second-author-" + suffix);
    User reader = user("reader-" + suffix);
    userRepository.save(author);
    userRepository.save(secondAuthor);
    userRepository.save(reader);
    userRepository.saveRelation(new FollowRelation(reader.getId(), author.getId()));

    Article newestArticle =
        article(
            "JPA Read Newest " + suffix,
            List.of("spring", "java"),
            author,
            Instant.parse("2026-06-16T12:00:00Z"));
    Article olderArticle =
        article(
            "JPA Read Older " + suffix,
            List.of("java"),
            secondAuthor,
            Instant.parse("2026-06-16T11:00:00Z"));
    articleRepository.save(newestArticle);
    articleRepository.save(olderArticle);
    articleFavoriteRepository.save(new ArticleFavorite(newestArticle.getId(), reader.getId()));
    commentRepository.save(
        Comment.restored(
            "comment-" + suffix,
            "synthetic comment",
            author.getId(),
            newestArticle.getId(),
            Instant.parse("2026-06-16T12:05:00Z")));

    ArticleDataList firstPage = articleQueryService.findRecentArticles(null, null, null, new Page(0, 1), reader);
    Assertions.assertEquals(2, firstPage.getCount());
    Assertions.assertEquals(1, firstPage.getArticleDatas().size());
    Assertions.assertEquals(newestArticle.getId(), firstPage.getArticleDatas().get(0).getId());

    ArticleDataList secondPage = articleQueryService.findRecentArticles(null, null, null, new Page(1, 1), reader);
    Assertions.assertEquals(2, secondPage.getCount());
    Assertions.assertEquals(olderArticle.getId(), secondPage.getArticleDatas().get(0).getId());

    ArticleDataList byTag = articleQueryService.findRecentArticles("spring", null, null, new Page(), reader);
    Assertions.assertEquals(1, byTag.getCount());
    assertArticleFlags(byTag.getArticleDatas().get(0), newestArticle, true, true, 1);

    ArticleDataList byAuthor =
        articleQueryService.findRecentArticles(null, author.getUsername(), null, new Page(), reader);
    Assertions.assertEquals(1, byAuthor.getCount());
    Assertions.assertEquals(newestArticle.getId(), byAuthor.getArticleDatas().get(0).getId());

    ArticleDataList byFavorited =
        articleQueryService.findRecentArticles(null, null, reader.getUsername(), new Page(), reader);
    Assertions.assertEquals(1, byFavorited.getCount());
    assertArticleFlags(byFavorited.getArticleDatas().get(0), newestArticle, true, true, 1);

    ArticleDataList feed = articleQueryService.findUserFeed(reader, new Page());
    Assertions.assertEquals(1, feed.getCount());
    assertArticleFlags(feed.getArticleDatas().get(0), newestArticle, true, true, 1);

    Optional<ArticleData> bySlug = articleQueryService.findBySlug(newestArticle.getSlug(), reader);
    Assertions.assertTrue(bySlug.isPresent());
    assertArticleFlags(bySlug.get(), newestArticle, true, true, 1);
    Assertions.assertTrue(bySlug.get().getTagList().contains("spring"));
    Assertions.assertTrue(bySlug.get().getTagList().contains("java"));

    Optional<ProfileData> authorProfile = profileQueryService.findByUsername(author.getUsername(), reader);
    Assertions.assertTrue(authorProfile.isPresent());
    Assertions.assertTrue(authorProfile.get().isFollowing());

    Assertions.assertTrue(tagsQueryService.allTags().contains("spring"));
    Assertions.assertTrue(tagsQueryService.allTags().contains("java"));

    List<CommentData> comments = commentQueryService.findByArticleId(newestArticle.getId(), reader);
    Assertions.assertEquals(1, comments.size());
    Assertions.assertEquals("synthetic comment", comments.get(0).getBody());
    Assertions.assertTrue(comments.get(0).getProfileData().isFollowing());

    assertDirectReadServiceMethods(newestArticle, olderArticle, author, reader, suffix);
  }

  private void assertDirectReadServiceMethods(
      Article newestArticle, Article olderArticle, User author, User reader, String suffix) {
    Assertions.assertEquals(newestArticle.getId(), articleReadService.findById(newestArticle.getId()).getId());
    Assertions.assertEquals(0, articleReadService.countFeedSize(List.of()));
    Assertions.assertTrue(articleReadService.findArticles(List.of()).isEmpty());
    Assertions.assertEquals(
        List.of(newestArticle.getId()),
        articleReadService.findArticlesWithCursor(
            "spring", null, null, new CursorPageParameter<>(null, 1, Direction.NEXT)));
    Assertions.assertEquals(
        1,
        articleReadService
            .findArticlesOfAuthorsWithCursor(
                List.of(author.getId()),
                new CursorPageParameter<>(Instant.parse("2026-06-16T13:00:00Z"), 1, Direction.NEXT))
            .size());
    Assertions.assertEquals(
        1,
        articleReadService
            .findArticlesOfAuthorsWithCursor(
                List.of(author.getId()),
                new CursorPageParameter<>(Instant.parse("2026-06-16T11:00:00Z"), 1, Direction.PREV))
            .size());

    Assertions.assertTrue(articleFavoritesReadService.isUserFavorite(reader.getId(), newestArticle.getId()));
    Assertions.assertEquals(1, articleFavoritesReadService.articleFavoriteCount(newestArticle.getId()));
    Assertions.assertEquals(
        1,
        articleFavoritesReadService
            .articlesFavoriteCount(List.of(newestArticle.getId(), olderArticle.getId()))
            .stream()
            .filter(count -> count.getId().equals(newestArticle.getId()) && count.getCount() == 1)
            .count());
    Assertions.assertTrue(articleFavoritesReadService.userFavorites(List.of(), reader).isEmpty());
    Assertions.assertTrue(
        articleFavoritesReadService
            .userFavorites(List.of(newestArticle.getId(), olderArticle.getId()), reader)
            .contains(newestArticle.getId()));

    Assertions.assertTrue(userRelationshipQueryService.isUserFollowing(reader.getId(), author.getId()));
    Assertions.assertTrue(userRelationshipQueryService.followingAuthors(reader.getId(), List.of()).isEmpty());
    Assertions.assertTrue(
        userRelationshipQueryService
            .followingAuthors(reader.getId(), List.of(author.getId()))
            .contains(author.getId()));
    Assertions.assertTrue(userRelationshipQueryService.followedUsers(reader.getId()).contains(author.getId()));

    Assertions.assertEquals(reader.getUsername(), userReadService.findById(reader.getId()).getUsername());
    Assertions.assertNull(userReadService.findByUsername("missing-" + reader.getUsername()));

    Assertions.assertEquals("synthetic comment", commentReadService.findById("comment-" + suffix).getBody());
    Assertions.assertEquals(
        1,
        commentReadService
            .findByArticleIdWithCursor(
                newestArticle.getId(),
                new CursorPageParameter<>(Instant.parse("2026-06-16T13:00:00Z"), 1, Direction.NEXT))
            .size());
  }

  private void assertJpaReadServicesArePrimaryBeans() {
    Assertions.assertInstanceOf(JpaArticleReadService.class, articleReadService);
    Assertions.assertInstanceOf(JpaArticleFavoritesReadService.class, articleFavoritesReadService);
    Assertions.assertInstanceOf(JpaUserRelationshipQueryService.class, userRelationshipQueryService);
    Assertions.assertInstanceOf(JpaUserReadService.class, userReadService);
    Assertions.assertInstanceOf(JpaTagReadService.class, tagReadService);
    Assertions.assertInstanceOf(JpaCommentReadService.class, commentReadService);
  }

  private void assertArticleFlags(
      ArticleData articleData,
      Article expectedArticle,
      boolean favorited,
      boolean following,
      int favoritesCount) {
    Assertions.assertEquals(expectedArticle.getId(), articleData.getId());
    Assertions.assertEquals(favorited, articleData.isFavorited());
    Assertions.assertEquals(favoritesCount, articleData.getFavoritesCount());
    Assertions.assertEquals(following, articleData.getProfileData().isFollowing());
  }

  private User user(String username) {
    return new User(
        username + "@example.com",
        username,
        "encoded-password",
        username + " bio",
        "https://example.com/" + username + ".png");
  }

  private Article article(String title, List<String> tags, User author, Instant createdAt) {
    return new Article(title, "description", "body", tags, author.getId(), createdAt);
  }
}
