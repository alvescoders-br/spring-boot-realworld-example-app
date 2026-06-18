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
import io.spring.infrastructure.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.readservice.ArticleReadService;
import io.spring.infrastructure.readservice.CommentReadService;
import io.spring.infrastructure.readservice.TagReadService;
import io.spring.infrastructure.readservice.UserReadService;
import io.spring.infrastructure.readservice.UserRelationshipQueryService;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("postgres")
@Transactional
@Sql(
    scripts = "/sql/truncate-all.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD,
    config = @SqlConfig(transactionMode = TransactionMode.ISOLATED))
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
  @Autowired private EntityManager entityManager;

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
    Assertions.assertEquals(1, bySlug.get().getReadingTime());
    assertCachedReadingTime(newestArticle.getId(), 1);
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

  @Test
  public void should_exclude_soft_deleted_articles_from_rest_read_paths() {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    User author = user("soft-delete-author-" + suffix);
    User reader = user("soft-delete-reader-" + suffix);
    userRepository.save(author);
    userRepository.save(reader);
    userRepository.saveRelation(new FollowRelation(reader.getId(), author.getId()));

    Article visibleArticle =
        article(
            "Soft Delete Visible " + suffix,
            List.of("visible"),
            author,
            Instant.parse("2026-06-18T12:00:00Z"));
    Article deletedArticle =
        article(
            "Soft Delete Hidden " + suffix,
            List.of("hidden"),
            author,
            Instant.parse("2026-06-18T11:00:00Z"));
    articleRepository.save(visibleArticle);
    articleRepository.save(deletedArticle);
    articleFavoriteRepository.save(new ArticleFavorite(deletedArticle.getId(), reader.getId()));

    articleRepository.remove(deletedArticle);

    Assertions.assertTrue(articleQueryService.findBySlug(deletedArticle.getSlug(), reader).isEmpty());
    Assertions.assertNull(articleReadService.findById(deletedArticle.getId()));
    Assertions.assertTrue(articleReadService.findArticles(List.of(deletedArticle.getId())).isEmpty());
    Assertions.assertEquals(0, articleFavoritesReadService.articleFavoriteCount(deletedArticle.getId()));
    Assertions.assertFalse(
        articleFavoritesReadService.isUserFavorite(reader.getId(), deletedArticle.getId()));

    ArticleDataList recentArticles =
        articleQueryService.findRecentArticles(null, null, null, new Page(), reader);
    Assertions.assertEquals(1, recentArticles.getCount());
    Assertions.assertEquals(visibleArticle.getId(), recentArticles.getArticleDatas().get(0).getId());

    ArticleDataList deletedByTag =
        articleQueryService.findRecentArticles("hidden", null, null, new Page(), reader);
    Assertions.assertEquals(0, deletedByTag.getCount());
    Assertions.assertTrue(deletedByTag.getArticleDatas().isEmpty());

    ArticleDataList deletedByFavorite =
        articleQueryService.findRecentArticles(null, null, reader.getUsername(), new Page(), reader);
    Assertions.assertEquals(0, deletedByFavorite.getCount());
    Assertions.assertTrue(deletedByFavorite.getArticleDatas().isEmpty());

    ArticleDataList feed = articleQueryService.findUserFeed(reader, new Page());
    Assertions.assertEquals(1, feed.getCount());
    Assertions.assertEquals(visibleArticle.getId(), feed.getArticleDatas().get(0).getId());
    Assertions.assertEquals(1, articleReadService.countFeedSize(List.of(author.getId())));
    Assertions.assertEquals(
        List.of(visibleArticle.getId()),
        articleReadService.findArticlesWithCursor(
            null, null, null, new CursorPageParameter<>(null, 10, Direction.NEXT)));
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

  private void assertCachedReadingTime(String articleId, int expectedReadingTime) {
    Object cachedReadingTime =
        entityManager
            .createNativeQuery("select reading_time from articles where id = :id")
            .setParameter("id", articleId)
            .getSingleResult();

    Assertions.assertEquals(expectedReadingTime, ((Number) cachedReadingTime).intValue());
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
