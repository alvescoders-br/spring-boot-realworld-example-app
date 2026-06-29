package io.spring.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
@Sql(
    scripts = "/sql/truncate-all.sql",
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD,
    config = @SqlConfig(transactionMode = TransactionMode.ISOLATED))
class RestGraphqlErrorHandlingIntegrationTest {

  @LocalServerPort private int port;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
  }

  @Test
  void restBusinessExceptionsShouldReturnRealWorldErrorBodies() {
    String suffix = uniqueSuffix();
    String token = registerUser("reader-" + suffix);

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/articles/missing-" + suffix)
        .then()
        .statusCode(404)
        .body("errors.body[0]", equalTo("not found"));

    given()
        .header("Authorization", "Token " + token)
        .when()
        .get("/profiles/missing-" + suffix)
        .then()
        .statusCode(404)
        .body("errors.body[0]", equalTo("not found"));

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + token)
        .body(articleParamWithoutTitle())
        .when()
        .post("/articles")
        .then()
        .statusCode(422)
        .body("errors.title[0]", equalTo("can't be empty"));
  }

  @Test
  void ownershipFailuresShouldReturnForbiddenRealWorldErrorBodies() {
    String suffix = uniqueSuffix();
    String authorToken = registerUser("author-" + suffix);
    String commenterToken = registerUser("commenter-" + suffix);
    String intruderToken = registerUser("intruder-" + suffix);
    String slug = createArticle(authorToken, "Owned article " + suffix, List.of("ownership"));

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + intruderToken)
        .body(updateArticleParam("Intruder update " + suffix))
        .when()
        .put("/articles/{slug}", slug)
        .then()
        .statusCode(403)
        .body("errors.body[0]", equalTo("forbidden"));

    String commentId =
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Token " + commenterToken)
            .body(commentParam("comment " + suffix))
            .when()
            .post("/articles/{slug}/comments", slug)
            .then()
            .statusCode(201)
            .extract()
            .path("comment.id");

    given()
        .header("Authorization", "Token " + intruderToken)
        .when()
        .delete("/articles/{slug}/comments/{id}", slug, commentId)
        .then()
        .statusCode(403)
        .body("errors.body[0]", equalTo("forbidden"));
  }

  @Test
  void restCreateArticleShouldTreatMissingOrNullTagListAsEmpty() {
    String suffix = uniqueSuffix();
    String token = registerUser("tags-" + suffix);

    assertArticleCreatedWithTags(token, "Missing tagList " + suffix, TagListCase.OMITTED, 0);
    assertArticleCreatedWithTags(token, "Null tagList " + suffix, TagListCase.NULL, 0);
    assertArticleCreatedWithTags(token, "Empty tagList " + suffix, TagListCase.EMPTY, 0);
    assertArticleCreatedWithTags(token, "Filled tagList " + suffix, TagListCase.FILLED, 1);
  }

  @Test
  void graphqlHttpTransportShouldUseMapperWithoutRootUnwrapping() {
    String suffix = uniqueSuffix();
    String token = registerUser("graphql-" + suffix);
    createArticle(token, "GraphQL HTTP article " + suffix, List.of("graphql-http"));

    Map<String, Object> request = new HashMap<>();
    request.put("query", "{ tags }");
    request.put("variables", Map.of());

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .post("/graphql")
        .then()
        .statusCode(200)
        .body("data.tags", notNullValue());
  }

  @Test
  void restRootEnvelopeShouldRemainEnabled() {
    String suffix = uniqueSuffix();
    String email = "login-" + suffix + "@example.com";
    String username = "login-" + suffix;
    String password = "password-" + suffix;

    given()
        .contentType(ContentType.JSON)
        .body(userParam(email, username, password))
        .when()
        .post("/users")
        .then()
        .statusCode(201)
        .body("user.email", equalTo(email))
        .body("user.token", notNullValue());

    given()
        .contentType(ContentType.JSON)
        .body(loginParam(email, password))
        .when()
        .post("/users/login")
        .then()
        .statusCode(200)
        .body("user.email", equalTo(email))
        .body("user.token", notNullValue());
  }

  private void assertArticleCreatedWithTags(
      String token, String title, TagListCase tagListCase, int expectedTagCount) {
    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + token)
        .body(articleParam(title, tagListCase))
        .when()
        .post("/articles")
        .then()
        .statusCode(200)
        .body("article.title", equalTo(title))
        .body("article.tagList.size()", equalTo(expectedTagCount));
  }

  private String registerUser(String username) {
    String email = username + "@example.com";
    String password = "password-" + username;
    return given()
        .contentType(ContentType.JSON)
        .body(userParam(email, username, password))
        .when()
        .post("/users")
        .then()
        .statusCode(201)
        .extract()
        .path("user.token");
  }

  private String createArticle(String token, String title, List<String> tags) {
    return given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + token)
        .body(articleParam(title, tags))
        .when()
        .post("/articles")
        .then()
        .statusCode(200)
        .extract()
        .path("article.slug");
  }

  private Map<String, Object> userParam(String email, String username, String password) {
    Map<String, Object> user = new HashMap<>();
    user.put("email", email);
    user.put("username", username);
    user.put("password", password);
    return Map.of("user", user);
  }

  private Map<String, Object> loginParam(String email, String password) {
    Map<String, Object> user = new HashMap<>();
    user.put("email", email);
    user.put("password", password);
    return Map.of("user", user);
  }

  private Map<String, Object> articleParam(String title, List<String> tags) {
    Map<String, Object> article = new HashMap<>();
    article.put("title", title);
    article.put("description", "description for " + title);
    article.put("body", "body for " + title);
    article.put("tagList", tags);
    return Map.of("article", article);
  }

  private Map<String, Object> articleParam(String title, TagListCase tagListCase) {
    Map<String, Object> article = new HashMap<>();
    article.put("title", title);
    article.put("description", "description for " + title);
    article.put("body", "body for " + title);
    if (tagListCase == TagListCase.NULL) {
      article.put("tagList", null);
    } else if (tagListCase == TagListCase.EMPTY) {
      article.put("tagList", List.of());
    } else if (tagListCase == TagListCase.FILLED) {
      article.put("tagList", List.of("x"));
    }
    return Map.of("article", article);
  }

  private Map<String, Object> articleParamWithoutTitle() {
    Map<String, Object> article = new HashMap<>();
    article.put("description", "description without title");
    article.put("body", "body without title");
    article.put("tagList", List.of());
    return Map.of("article", article);
  }

  private Map<String, Object> updateArticleParam(String title) {
    Map<String, Object> article = new HashMap<>();
    article.put("title", title);
    article.put("description", "updated description");
    article.put("body", "updated body");
    return Map.of("article", article);
  }

  private Map<String, Object> commentParam(String body) {
    return Map.of("comment", Map.of("body", body));
  }

  private String uniqueSuffix() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  }

  private enum TagListCase {
    OMITTED,
    NULL,
    EMPTY,
    FILLED
  }
}
