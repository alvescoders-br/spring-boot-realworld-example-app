package io.spring.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
@EnabledIfEnvironmentVariable(named = "REALWORLD_POSTGRES_URL", matches = "jdbc:postgresql:.*")
@Sql(
    scripts = "/sql/truncate-all.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD,
    config = @SqlConfig(transactionMode = TransactionMode.ISOLATED))
// quality/playwright-pass: baseline -- all public read-only assertions nominal. (#38)
class PlaywrightApiE2eTest {

  @LocalServerPort private int port;

  @Autowired private DgsQueryExecutor dgsQueryExecutor;
  @Autowired private UserRepository userRepository;
  @Autowired private ArticleRepository articleRepository;

  @Test
  void publicRestAndGraphqlReadOnlyPathsShouldExposeEquivalentArticleAndTagData() throws Exception {
    PublicReadFixture fixture = seedPublicReadFixture();

    try (Playwright playwright = Playwright.create()) {
      APIRequestContext request = newRequestContext(playwright);
      try {
        assertRestPublicReads(request, fixture);
      } finally {
        request.dispose();
      }
    }

    assertGraphqlPublicReads();
  }

  private APIRequestContext newRequestContext(Playwright playwright) {
    return playwright
        .request()
        .newContext(new APIRequest.NewContextOptions().setBaseURL("http://localhost:" + port));
  }

  private void assertRestPublicReads(APIRequestContext request, PublicReadFixture fixture) {
    APIResponse articlesResponse = request.get("/articles?limit=10");
    assertThat(articlesResponse.status()).isEqualTo(200);
    DocumentContext articlesJson = JsonPath.parse(articlesResponse.text());

    assertThat((Integer) articlesJson.read("$.articlesCount")).isEqualTo(2);
    assertThat((List<String>) articlesJson.read("$.articles[*].slug"))
        .contains(fixture.newestArticle().getSlug(), fixture.olderArticle().getSlug());
    assertThat((String) articlesJson.read("$.articles[0].slug"))
        .isEqualTo(fixture.newestArticle().getSlug());
    assertThat((List<String>) articlesJson.read("$.articles[0].tagList"))
        .contains("playwright", "rest");
    assertThat((Integer) articlesJson.read("$.articles[0].readingTime")).isEqualTo(2);

    APIResponse tagsResponse = request.get("/tags");
    assertThat(tagsResponse.status()).isEqualTo(200);
    DocumentContext tagsJson = JsonPath.parse(tagsResponse.text());
    assertThat((List<String>) tagsJson.read("$.tags"))
        .contains("playwright", "rest", "graphql");
  }

  private void assertGraphqlPublicReads() {
    ExecutionResult result = dgsQueryExecutor.execute("query PublicReadSmoke { tags }");
    assertThat(result.getErrors()).isEmpty();
    java.util.Map<String, Object> graphqlData = result.getData();

    assertThat((List<String>) graphqlData.get("tags")).contains("playwright", "rest", "graphql");
  }

  private PublicReadFixture seedPublicReadFixture() {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    User author =
        new User(
            "playwright-author-" + suffix + "@example.com",
            "playwright-author-" + suffix,
            "irrelevant-password",
            "Synthetic public-read fixture author",
            null);
    userRepository.save(author);

    Article newestArticle =
        new Article(
            "Playwright Public Newest " + suffix,
            "Newest public read-only E2E article",
            "word ".repeat(201).trim(),
            List.of("playwright", "rest"),
            author.getId(),
            Instant.parse("2026-06-18T12:00:00Z"));
    Article olderArticle =
        new Article(
            "Playwright Public Older " + suffix,
            "Older public read-only E2E article",
            "graphql smoke body",
            List.of("playwright", "graphql"),
            author.getId(),
            Instant.parse("2026-06-18T11:00:00Z"));

    articleRepository.save(newestArticle);
    articleRepository.save(olderArticle);

    return new PublicReadFixture(author, newestArticle, olderArticle);
  }

  private record PublicReadFixture(User author, Article newestArticle, Article olderArticle) {}
}
