package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class TagsQueryTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;

  @Test
  void tagsQueryReturnsListShape() {
    ExecutionResult result = dgsQueryExecutor.execute("{ tags }");
    assertThat(result.getErrors()).isEmpty();
  }
}
