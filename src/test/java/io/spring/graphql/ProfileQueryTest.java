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
public class ProfileQueryTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Test
  void profileQueryReturnsProfileShape() {
    dgsQueryExecutor.execute(
        "mutation { createUser(input: {"
            + " email: \"profile.test@example.com\","
            + " username: \"profiletestuser\","
            + " password: \"password123\""
            + " }) { __typename } }");

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ profile(username: \"profiletestuser\") {"
                + " profile { username bio image following } } }");
    assertThat(result.getErrors()).isEmpty();
    Map<String, Object> data = result.getData();
    Map<?, ?> profilePayload = (Map<?, ?>) data.get("profile");
    Map<?, ?> profile = (Map<?, ?>) profilePayload.get("profile");
    assertThat(profile.get("username")).isEqualTo("profiletestuser");
    assertThat(profile.get("following")).isEqualTo(false);
  }
}
