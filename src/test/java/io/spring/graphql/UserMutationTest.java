package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@WithAnonymousUser
public class UserMutationTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Test
  void createUserMutationReturnsUserPayloadShape() {
    String mutation =
        "mutation { createUser(input: {"
            + " email: \"dgstest1@example.com\","
            + " username: \"dgstest1user\","
            + " password: \"password123\""
            + " }) {"
            + " ... on UserPayload { user { email username token } }"
            + " ... on Error { message }"
            + " } }";
    ExecutionResult result = dgsQueryExecutor.execute(mutation);
    assertThat(result.getErrors()).isEmpty();
    Map<String, Object> data = result.getData();
    Map<?, ?> createUser = (Map<?, ?>) data.get("createUser");
    Map<?, ?> user = (Map<?, ?>) createUser.get("user");
    assertThat(user.get("email")).isEqualTo("dgstest1@example.com");
    assertThat(user.get("username")).isEqualTo("dgstest1user");
    assertThat(user.get("token")).isNotNull();
  }

  @Test
  void loginMutationReturnsUserPayloadWithToken() {
    dgsQueryExecutor.execute(
        "mutation { createUser(input: {"
            + " email: \"dgslogin@example.com\","
            + " username: \"dgsloginuser\","
            + " password: \"loginpassword\""
            + " }) { __typename } }");

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "mutation { login(email: \"dgslogin@example.com\", password: \"loginpassword\") {"
                + " user { email username token } } }");
    assertThat(result.getErrors()).isEmpty();
    Map<String, Object> data = result.getData();
    Map<?, ?> login = (Map<?, ?>) data.get("login");
    Map<?, ?> user = (Map<?, ?>) login.get("user");
    assertThat(user.get("email")).isEqualTo("dgslogin@example.com");
    assertThat(user.get("token")).isNotNull();
  }
}
