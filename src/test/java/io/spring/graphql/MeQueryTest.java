package io.spring.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.netflix.graphql.dgs.DgsQueryExecutor;
import graphql.ExecutionResult;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@SpringBootTest
@ActiveProfiles("test")
public class MeQueryTest {

  @Autowired private DgsQueryExecutor dgsQueryExecutor;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser =
        new User(
            "me.test@example.com",
            "metestuser",
            passwordEncoder.encode("password123"),
            "",
            "");
    userRepository.save(testUser);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void meQueryReturnsCurrentUserShape() {
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    mockRequest.addHeader("Authorization", "Token dummytoken");
    WebRequest webRequest = new ServletWebRequest(mockRequest);
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token dummytoken");

    ExecutionResult result =
        dgsQueryExecutor.execute(
            "{ me { email username token } }",
            Collections.emptyMap(),
            null,
            headers,
            null,
            webRequest);
    assertThat(result.getErrors()).isEmpty();
    Map<String, Object> data = result.getData();
    Map<?, ?> me = (Map<?, ?>) data.get("me");
    assertThat(me.get("email")).isEqualTo("me.test@example.com");
    assertThat(me.get("username")).isEqualTo("metestuser");
    assertThat(me.get("token")).isEqualTo("dummytoken");
  }
}
