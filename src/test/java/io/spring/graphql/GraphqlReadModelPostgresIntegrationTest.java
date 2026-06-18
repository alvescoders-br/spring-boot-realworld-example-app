package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.comment.Comment;
import io.spring.core.comment.CommentRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.jpa.readservice.JpaArticleFavoritesReadService;
import io.spring.infrastructure.jpa.readservice.JpaArticleReadService;
import io.spring.infrastructure.jpa.readservice.JpaCommentReadService;
import io.spring.infrastructure.jpa.readservice.JpaTagReadService;
import io.spring.infrastructure.jpa.readservice.JpaUserReadService;
import io.spring.infrastructure.jpa.readservice.JpaUserRelationshipQueryService;
import io.spring.infrastructure.readservice.ArticleFavoritesReadService;
import io.spring.infrastructure.readservice.ArticleReadService;
import io.spring.infrastructure.readservice.CommentReadService;
import io.spring.infrastructure.readservice.TagReadService;
import io.spring.infrastructure.readservice.UserReadService;
import io.spring.infrastructure.readservice.UserRelationshipQueryService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("postgres")
@Transactional
@EnabledIfEnvironmentVariable(named = "REALWORLD_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class GraphqlReadModelPostgresIntegrationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;
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

  private User author;
  private User secondAuthor;
  private User reader;
  private Article newestArticle;
  private Article olderArticle;
  private Comment newestComment;
  private Comment olderComment;

  @BeforeEach
  void setUp() {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    author = user("graphql-author-" + suffix);
    secondAuthor = user("graphql-second-" + suffix);
    reader = user("graphql-reader-" + suffix);
    userRepository.save(author);
    userRepository.save(secondAuthor);
    userRepository.save(reader);

    userRepository.saveRelation(new FollowRelation(reader.getId(), author.getId()));
    userRepository.saveRelation(new FollowRelation(author.getId(), secondAuthor.getId()));

    newestArticle =
        article(
            "GraphQL newest " + suffix,
            "word ".repeat(201).trim(),
            List.of("spring", "java"),
            author,
            Instant.parse("2026-06-17T12:00:00Z"));
    olderArticle =
        article(
            "GraphQL older " + suffix,
            List.of("graphql", "java"),
            secondAuthor,
            Instant.parse("2026-06-17T11:00:00Z"));
    articleRepository.save(newestArticle);
    articleRepository.save(olderArticle);

    articleFavoriteRepository.save(new ArticleFavorite(newestArticle.getId(), reader.getId()));
    articleFavoriteRepository.save(new ArticleFavorite(olderArticle.getId(), author.getId()));

    newestComment =
        Comment.restored(
            "comment-new-" + suffix,
            "newest comment",
            author.getId(),
            newestArticle.getId(),
            Instant.parse("2026-06-17T12:10:00Z"));
    olderComment =
        Comment.restored(
            "comment-old-" + suffix,
            "older comment",
            secondAuthor.getId(),
            newestArticle.getId(),
            Instant.parse("2026-06-17T12:05:00Z"));
    commentRepository.save(newestComment);
    commentRepository.save(olderComment);

    authenticateAs(reader);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void graphqlArticlesAndFeedShouldUseJpaReadServicesAndCursorPagination() {
    assertJpaReadServicesArePrimaryBeans();

    Map<String, Object> articlesQuery =
        executeData(
            "{ articles(first: 1) {"
                + " edges { cursor node { slug title tagList favorited favoritesCount readingTime author { username following } } }"
                + " pageInfo { hasNextPage hasPreviousPage startCursor endCursor }"
                + " } }");

    Map<String, Object> articlesConnection = mapValue(articlesQuery, "articles");
    List<Map<String, Object>> firstPageEdges = mapList(articlesConnection, "edges");
    Map<String, Object> firstArticleNode = mapValue(firstPageEdges.get(0), "node");
    Map<String, Object> firstPageInfo = mapValue(articlesConnection, "pageInfo");
    String firstPageCursor = (String) firstPageInfo.get("endCursor");

    assertThat(firstArticleNode.get("slug")).isEqualTo(newestArticle.getSlug());
    assertThat(firstArticleNode.get("favorited")).isEqualTo(true);
    assertThat(firstArticleNode.get("favoritesCount")).isEqualTo(1);
    assertThat(firstArticleNode.get("readingTime")).isEqualTo(2);
    assertThat((List<String>) firstArticleNode.get("tagList")).containsExactlyInAnyOrder("spring", "java");
    assertThat(mapValue(firstArticleNode, "author")).containsEntry("username", author.getUsername());
    assertThat(mapValue(firstArticleNode, "author")).containsEntry("following", true);
    assertThat(firstPageCursor).matches("\\d+");
    assertThat(firstPageInfo).containsEntry("hasNextPage", true);
    assertThat(firstPageInfo).containsEntry("hasPreviousPage", false);

    Map<String, Object> nextPageQuery =
        executeData(
            String.format(
                "{ articles(first: 1, after: \"%s\") {"
                    + " edges { cursor node { slug author { username } } }"
                    + " pageInfo { hasNextPage hasPreviousPage startCursor endCursor }"
                    + " } }",
                firstPageCursor));

    Map<String, Object> nextPageConnection = mapValue(nextPageQuery, "articles");
    List<Map<String, Object>> nextPageEdges = mapList(nextPageConnection, "edges");
    Map<String, Object> secondArticleNode = mapValue(nextPageEdges.get(0), "node");
    String secondPageCursor = (String) mapValue(nextPageConnection, "pageInfo").get("startCursor");

    assertThat(secondArticleNode.get("slug")).isEqualTo(olderArticle.getSlug());
    assertThat(mapValue(secondArticleNode, "author")).containsEntry("username", secondAuthor.getUsername());
    assertThat(mapValue(nextPageConnection, "pageInfo")).containsEntry("hasNextPage", false);
    assertThat(mapValue(nextPageConnection, "pageInfo")).containsEntry("hasPreviousPage", true);

    Map<String, Object> previousPageQuery =
        executeData(
            String.format(
                "{ articles(last: 1, before: \"%s\") {"
                    + " edges { node { slug } }"
                    + " pageInfo { hasNextPage hasPreviousPage }"
                    + " } }",
                secondPageCursor));

    Map<String, Object> previousPageConnection = mapValue(previousPageQuery, "articles");
    List<Map<String, Object>> previousPageEdges = mapList(previousPageConnection, "edges");
    assertThat(mapValue(previousPageEdges.get(0), "node")).containsEntry("slug", newestArticle.getSlug());

    Map<String, Object> feedQuery =
        executeData(
            "{ feed(first: 1) {"
                + " edges { node { slug favorited favoritesCount author { username following } } }"
                + " pageInfo { hasNextPage hasPreviousPage }"
                + " } }");

    Map<String, Object> feedConnection = mapValue(feedQuery, "feed");
    List<Map<String, Object>> feedEdges = mapList(feedConnection, "edges");
    Map<String, Object> feedNode = mapValue(feedEdges.get(0), "node");

    assertThat(feedNode.get("slug")).isEqualTo(newestArticle.getSlug());
    assertThat(feedNode.get("favorited")).isEqualTo(true);
    assertThat(feedNode.get("favoritesCount")).isEqualTo(1);
    assertThat(mapValue(feedNode, "author")).containsEntry("username", author.getUsername());
    assertThat(mapValue(feedNode, "author")).containsEntry("following", true);
  }

  @Test
  void graphqlDeleteArticleShouldSoftDeleteAndHideArticleFromQueries() {
    authenticateAs(author);

    Map<String, Object> deleteData =
        executeData(
            String.format(
                "mutation { deleteArticle(slug: \"%s\") { success } }", newestArticle.getSlug()));
    Map<String, Object> deletionStatus = mapValue(deleteData, "deleteArticle");
    assertThat(deletionStatus).containsEntry("success", true);
    assertThat(articleRepository.findById(newestArticle.getId())).isEmpty();

    ExecutionResult deletedArticleResult =
        dgsQueryExecutor.execute(
            String.format("{ article(slug: \"%s\") { slug } }", newestArticle.getSlug()));
    assertThat(deletedArticleResult.getErrors()).isNotEmpty();

    Map<String, Object> articlesData =
        executeData("{ articles(first: 5) { edges { node { slug } } } }");
    List<Map<String, Object>> articleEdges = mapList(mapValue(articlesData, "articles"), "edges");
    assertThat(articleEdges)
        .extracting(edge -> (String) mapValue(edge, "node").get("slug"))
        .contains(olderArticle.getSlug())
        .doesNotContain(newestArticle.getSlug());
  }

  @Test
  void graphqlProfileAndTagsShouldPreserveReadModelShape() {
    Map<String, Object> data =
        executeData(
            String.format(
                "{ profile(username: \"%s\") {"
                    + " profile {"
                    + " username following"
                    + " articles(first: 1) { edges { node { slug } } }"
                    + " favorites(first: 1) { edges { node { slug favorited author { username following } } } }"
                    + " feed(first: 1) { edges { node { slug favorited author { username following } } } }"
                    + " }"
                    + " } tags }",
                author.getUsername()));

    Map<String, Object> profilePayload = mapValue(data, "profile");
    Map<String, Object> profile = mapValue(profilePayload, "profile");
    List<String> tags = (List<String>) data.get("tags");

    assertThat(profile.get("username")).isEqualTo(author.getUsername());
    assertThat(profile.get("following")).isEqualTo(true);

    Map<String, Object> profileArticles = mapValue(profile, "articles");
    Map<String, Object> profileFavorites = mapValue(profile, "favorites");
    Map<String, Object> profileFeed = mapValue(profile, "feed");

    assertThat(mapValue(mapList(profileArticles, "edges").get(0), "node"))
        .containsEntry("slug", newestArticle.getSlug());

    Map<String, Object> favoriteNode = mapValue(mapList(profileFavorites, "edges").get(0), "node");
    assertThat(favoriteNode.get("slug")).isEqualTo(olderArticle.getSlug());
    assertThat(favoriteNode.get("favorited")).isEqualTo(false);
    assertThat(mapValue(favoriteNode, "author")).containsEntry("username", secondAuthor.getUsername());

    Map<String, Object> feedNode = mapValue(mapList(profileFeed, "edges").get(0), "node");
    assertThat(feedNode.get("slug")).isEqualTo(olderArticle.getSlug());
    assertThat(feedNode.get("favorited")).isEqualTo(true);
    assertThat(mapValue(feedNode, "author")).containsEntry("username", secondAuthor.getUsername());
    assertThat(mapValue(feedNode, "author")).containsEntry("following", false);

    assertThat(tags).contains("spring", "java", "graphql");
  }

  @Test
  void graphqlArticleCommentsShouldPreserveCursorPaginationAndArticleLink() {
    Map<String, Object> firstPageQuery =
        executeData(
            String.format(
                "{ article(slug: \"%s\") {"
                    + " slug"
                    + " comments(first: 1) {"
                    + " edges { cursor node { id body createdAt updatedAt author { username following } article { slug } } }"
                    + " pageInfo { hasNextPage hasPreviousPage startCursor endCursor }"
                    + " }"
                    + " } }",
                newestArticle.getSlug()));

    Map<String, Object> article = mapValue(firstPageQuery, "article");
    Map<String, Object> firstCommentsConnection = mapValue(article, "comments");
    List<Map<String, Object>> firstCommentEdges = mapList(firstCommentsConnection, "edges");
    Map<String, Object> firstCommentNode = mapValue(firstCommentEdges.get(0), "node");
    Map<String, Object> firstCommentsPageInfo = mapValue(firstCommentsConnection, "pageInfo");
    String firstCommentCursor = (String) firstCommentsPageInfo.get("endCursor");

    assertThat(article.get("slug")).isEqualTo(newestArticle.getSlug());
    assertThat(firstCommentNode.get("id")).isEqualTo(newestComment.getId());
    assertThat(firstCommentNode.get("body")).isEqualTo("newest comment");
    assertThat(firstCommentNode.get("createdAt")).isEqualTo("2026-06-17T12:10:00.000Z");
    assertThat(firstCommentNode.get("updatedAt")).isEqualTo("2026-06-17T12:10:00.000Z");
    assertThat(mapValue(firstCommentNode, "author")).containsEntry("username", author.getUsername());
    assertThat(mapValue(firstCommentNode, "author")).containsEntry("following", true);
    assertThat(mapValue(firstCommentNode, "article")).containsEntry("slug", newestArticle.getSlug());
    assertThat(firstCommentCursor).matches("\\d+");
    assertThat(firstCommentsPageInfo).containsEntry("hasNextPage", true);
    assertThat(firstCommentsPageInfo).containsEntry("hasPreviousPage", false);

    Map<String, Object> nextPageQuery =
        executeData(
            String.format(
                "{ article(slug: \"%s\") {"
                    + " comments(first: 1, after: \"%s\") {"
                    + " edges { cursor node { id body author { username following } article { slug } } }"
                    + " pageInfo { hasNextPage hasPreviousPage startCursor endCursor }"
                    + " }"
                    + " } }",
                newestArticle.getSlug(), firstCommentCursor));

    Map<String, Object> secondCommentsConnection =
        mapValue(mapValue(nextPageQuery, "article"), "comments");
    List<Map<String, Object>> secondCommentEdges = mapList(secondCommentsConnection, "edges");
    Map<String, Object> secondCommentNode = mapValue(secondCommentEdges.get(0), "node");
    String secondCommentCursor = (String) mapValue(secondCommentsConnection, "pageInfo").get("startCursor");

    assertThat(secondCommentNode.get("id")).isEqualTo(olderComment.getId());
    assertThat(secondCommentNode.get("body")).isEqualTo("older comment");
    assertThat(mapValue(secondCommentNode, "author")).containsEntry("username", secondAuthor.getUsername());
    assertThat(mapValue(secondCommentNode, "author")).containsEntry("following", false);
    assertThat(mapValue(secondCommentNode, "article")).containsEntry("slug", newestArticle.getSlug());
    assertThat(mapValue(secondCommentsConnection, "pageInfo")).containsEntry("hasNextPage", false);
    assertThat(mapValue(secondCommentsConnection, "pageInfo")).containsEntry("hasPreviousPage", true);

    Map<String, Object> previousPageQuery =
        executeData(
            String.format(
                "{ article(slug: \"%s\") {"
                    + " comments(last: 1, before: \"%s\") {"
                    + " edges { node { id body } }"
                    + " pageInfo { hasNextPage hasPreviousPage }"
                    + " }"
                    + " } }",
                newestArticle.getSlug(), secondCommentCursor));

    Map<String, Object> previousCommentsConnection =
        mapValue(mapValue(previousPageQuery, "article"), "comments");
    List<Map<String, Object>> previousCommentEdges = mapList(previousCommentsConnection, "edges");

    assertThat(mapValue(previousCommentEdges.get(0), "node")).containsEntry("id", newestComment.getId());
    assertThat(mapValue(previousCommentEdges.get(0), "node")).containsEntry("body", "newest comment");
  }

  private void assertJpaReadServicesArePrimaryBeans() {
    assertThat(articleReadService).isInstanceOf(JpaArticleReadService.class);
    assertThat(articleFavoritesReadService).isInstanceOf(JpaArticleFavoritesReadService.class);
    assertThat(userRelationshipQueryService).isInstanceOf(JpaUserRelationshipQueryService.class);
    assertThat(userReadService).isInstanceOf(JpaUserReadService.class);
    assertThat(tagReadService).isInstanceOf(JpaTagReadService.class);
    assertThat(commentReadService).isInstanceOf(JpaCommentReadService.class);
  }

  private User user(String username) {
    return new User(
        username + "@example.com",
        username,
        "encoded-password",
        username + " bio",
        "https://example.com/" + username + ".png");
  }

  private Article article(String title, List<String> tags, User articleAuthor, Instant createdAt) {
    return article(title, "body", tags, articleAuthor, createdAt);
  }

  private Article article(
      String title, String body, List<String> tags, User articleAuthor, Instant createdAt) {
    return new Article(title, "description", body, tags, articleAuthor.getId(), createdAt);
  }

  private void authenticateAs(User currentUser) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(currentUser, null, Collections.emptyList());
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  private Map<String, Object> executeData(String query) {
    ExecutionResult result = dgsQueryExecutor.execute(query);
    assertThat(result.getErrors()).isEmpty();
    @SuppressWarnings("unchecked")
    Map<String, Object> data = result.getData();
    return data;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mapValue(Map<String, Object> source, String key) {
    return (Map<String, Object>) source.get(key);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> mapList(Map<String, Object> source, String key) {
    return (List<Map<String, Object>>) source.get(key);
  }
}
