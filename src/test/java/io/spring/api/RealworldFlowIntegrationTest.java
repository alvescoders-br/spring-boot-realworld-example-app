package io.spring.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Phase 2 — Slice 3: Full RealWorld flow integration test.
 *
 * <p>Sobe o servidor real (@SpringBootTest RANDOM_PORT + RestAssured standalone) e encadeia os
 * principais endpoints do contrato RealWorld, verificando shape e envelope de cada resposta. Não
 * altera nenhum código de produção. Issue de tracking: #7.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("postgres")
public class RealworldFlowIntegrationTest {

  @LocalServerPort private int port;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
  }

  @Test
  void fullRealworldFlowShouldPreserveContractEnvelopes() {
    // Unique identifiers per run to avoid conflicts with persisted test state
    String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String email = "flow-" + uniqueSuffix + "@example.com";
    String username = "user-" + uniqueSuffix;
    String password = "password-" + uniqueSuffix;

    // --- Step 1: Register ---
    Map<String, Object> registerBody = buildUserParam(email, username, password);

    String token =
        given()
            .contentType(ContentType.JSON)
            .body(registerBody)
            .when()
            .post("/users")
            .then()
            .statusCode(201)
            .body("user", notNullValue())
            .body("user.email", equalTo(email))
            .body("user.username", equalTo(username))
            .body("user.token", notNullValue())
            .extract()
            .path("user.token");

    // --- Step 2: Login ---
    Map<String, Object> loginBody = buildLoginParam(email, password);

    String loginToken =
        given()
            .contentType(ContentType.JSON)
            .body(loginBody)
            .when()
            .post("/users/login")
            .then()
            .statusCode(200)
            .body("user", notNullValue())
            .body("user.email", equalTo(email))
            .body("user.username", equalTo(username))
            .body("user.token", notNullValue())
            .extract()
            .path("user.token");

    // --- Step 3: Current user (GET /user) ---
    given()
        .header("Authorization", "Token " + loginToken)
        .when()
        .get("/user")
        .then()
        .statusCode(200)
        .body("user", notNullValue())
        .body("user.email", equalTo(email))
        .body("user.username", equalTo(username));

    // --- Step 4: Create article (POST /articles) ---
    Map<String, Object> articleBody = buildArticleParam(
        "Flow Test Article " + uniqueSuffix,
        "Flow test description",
        "Flow test body content",
        Arrays.asList("flow", "test"));

    String articleSlug =
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Token " + loginToken)
            .body(articleBody)
            .when()
            .post("/articles")
            .then()
            .statusCode(200)
            .body("article", notNullValue())
            .body("article.slug", notNullValue())
            .body("article.title", notNullValue())
            .body("article.author", notNullValue())
            .extract()
            .path("article.slug");

    // --- Step 5: Read article back (GET /articles/{slug}) ---
    given()
        .header("Authorization", "Token " + loginToken)
        .when()
        .get("/articles/" + articleSlug)
        .then()
        .statusCode(200)
        .body("article", notNullValue())
        .body("article.slug", equalTo(articleSlug))
        .body("article", hasKey("title"))
        .body("article", hasKey("description"))
        .body("article", hasKey("body"))
        .body("article", hasKey("author"))
        .body("article", hasKey("favorited"))
        .body("article", hasKey("favoritesCount"));

    // --- Step 6: Add comment (POST /articles/{slug}/comments) ---
    Map<String, Object> commentBody = buildCommentParam("Great flow test article!");

    given()
        .contentType(ContentType.JSON)
        .header("Authorization", "Token " + loginToken)
        .body(commentBody)
        .when()
        .post("/articles/" + articleSlug + "/comments")
        .then()
        .statusCode(201)
        .body("comment", notNullValue())
        .body("comment.body", equalTo("Great flow test article!"))
        .body("comment", hasKey("id"))
        .body("comment", hasKey("author"));

    // --- Step 7: Favorite article (POST /articles/{slug}/favorite) ---
    given()
        .header("Authorization", "Token " + loginToken)
        .when()
        .post("/articles/" + articleSlug + "/favorite")
        .then()
        .statusCode(200)
        .body("article", notNullValue())
        .body("article.slug", equalTo(articleSlug))
        .body("article.favorited", equalTo(true))
        .body("article.favoritesCount", equalTo(1));

    // --- Step 8: Unfavorite article (DELETE /articles/{slug}/favorite) ---
    given()
        .header("Authorization", "Token " + loginToken)
        .when()
        .delete("/articles/" + articleSlug + "/favorite")
        .then()
        .statusCode(200)
        .body("article", notNullValue())
        .body("article.slug", equalTo(articleSlug))
        .body("article.favorited", equalTo(false))
        .body("article.favoritesCount", equalTo(0));

    // --- Step 9: Get profile (GET /profiles/{username}) ---
    given()
        .header("Authorization", "Token " + loginToken)
        .when()
        .get("/profiles/" + username)
        .then()
        .statusCode(200)
        .body("profile", notNullValue())
        .body("profile.username", equalTo(username))
        .body("profile", hasKey("following"))
        .body("profile", hasKey("bio"))
        .body("profile", hasKey("image"));
  }

  // --- Payload helpers ---

  private Map<String, Object> buildUserParam(
      final String email, final String username, final String password) {
    Map<String, Object> userFields = new HashMap<>();
    userFields.put("email", email);
    userFields.put("username", username);
    userFields.put("password", password);
    Map<String, Object> wrapper = new HashMap<>();
    wrapper.put("user", userFields);
    return wrapper;
  }

  private Map<String, Object> buildLoginParam(final String email, final String password) {
    Map<String, Object> userFields = new HashMap<>();
    userFields.put("email", email);
    userFields.put("password", password);
    Map<String, Object> wrapper = new HashMap<>();
    wrapper.put("user", userFields);
    return wrapper;
  }

  private Map<String, Object> buildArticleParam(
      final String title,
      final String description,
      final String body,
      final java.util.List<String> tagList) {
    Map<String, Object> articleFields = new HashMap<>();
    articleFields.put("title", title);
    articleFields.put("description", description);
    articleFields.put("body", body);
    articleFields.put("tagList", tagList);
    Map<String, Object> wrapper = new HashMap<>();
    wrapper.put("article", articleFields);
    return wrapper;
  }

  private Map<String, Object> buildCommentParam(final String body) {
    Map<String, Object> commentFields = new HashMap<>();
    commentFields.put("body", body);
    Map<String, Object> wrapper = new HashMap<>();
    wrapper.put("comment", commentFields);
    return wrapper;
  }
}
