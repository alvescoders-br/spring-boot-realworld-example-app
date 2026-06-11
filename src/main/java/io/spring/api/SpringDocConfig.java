package io.spring.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@Configuration
public class SpringDocConfig {

  @Bean
  public WebSecurityCustomizer springDocSecurityCustomizer() {
    return web ->
        web.ignoring()
            .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");
  }
}
