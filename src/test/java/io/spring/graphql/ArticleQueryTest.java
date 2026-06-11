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
public class ArticleQueryTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Test
  void articlesQueryReturnsConnectionShape() {
    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ articles(first: 5) {"
                + " edges { cursor node { slug title } }"
                + " pageInfo { hasNextPage hasPreviousPage }"
                + " } }");
    assertThat(result.getErrors()).isEmpty();
    Map<String, Object> data = result.getData();
    assertThat(data).containsKey("articles");
    @SuppressWarnings("unchecked")
    Map<String, Object> articles = (Map<String, Object>) data.get("articles");
    assertThat(articles).containsKey("pageInfo");
    assertThat(articles).containsKey("edges");
  }
}
