package io.spring.graphql;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.webmvc.GraphQlHttpHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dedicated HTTP handler for the Spring GraphQL endpoint.
 *
 * <p>The REST layer relies on a globally enabled {@code
 * DeserializationFeature.UNWRAP_ROOT_VALUE} (see {@code
 * spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true} in {@code
 * application.properties}) so that {@code @JsonRootName} envelopes such as {@code
 * {"user":{...}}} deserialize into the REST params.
 *
 * <p>Spring Boot autoconfigures {@link GraphQlHttpHandler} with a {@code null}
 * message converter, which makes it fall back to the shared MVC {@code
 * HttpMessageConverters} — i.e. the primary {@code ObjectMapper} with root
 * unwrapping enabled. A GraphQL POST body ({@code {"query":...}}) has no root
 * wrapper, so that shared converter fails with {@code HttpMessageNotReadableException}
 * and the endpoint answers {@code 400 "Failed to read request"}.
 *
 * <p>This bean overrides the autoconfigured handler ({@code @ConditionalOnMissingBean})
 * and hands it a dedicated JSON converter backed by a clean {@link JsonMapper}
 * <em>without</em> root unwrapping, so the GraphQL transport reads/writes plain
 * JSON while the REST envelopes keep working untouched.
 */
@Configuration
public class GraphQlHttpConfig {

  @Bean
  public GraphQlHttpHandler graphQlHttpHandler(WebGraphQlHandler webGraphQlHandler) {
    JsonMapper graphQlJsonMapper = JsonMapper.builder().build();
    JacksonJsonHttpMessageConverter converter =
        new JacksonJsonHttpMessageConverter(graphQlJsonMapper);
    converter.setSupportedMediaTypes(
        List.of(
            MediaType.APPLICATION_JSON,
            MediaType.parseMediaType("application/graphql-response+json"),
            MediaType.parseMediaType("application/*+json")));
    return new GraphQlHttpHandler(webGraphQlHandler, converter);
  }
}
