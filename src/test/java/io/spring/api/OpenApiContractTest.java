package io.spring.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OpenApiContractTest {

  @LocalServerPort private int port;

  @BeforeEach
  void setUp() {
    RestAssured.port = port;
  }

  @Test
  void apiDocsShouldReturnOpenApiDocumentWithMainPaths() {
    given()
        .when()
        .get("/v3/api-docs")
        .then()
        .statusCode(200)
        .body("paths", hasKey("/users"))
        .body("paths", hasKey("/user"))
        .body("paths", hasKey("/articles"))
        .body("paths", hasKey("/profiles/{username}"))
        .body("paths", hasKey("/tags"));
  }
}
