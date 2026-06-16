package io.spring;

import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonCustomizations {

  @Bean
  public SimpleModule realWorldModules() {
    return new RealWorldModules();
  }

  public static class RealWorldModules extends SimpleModule {
    public RealWorldModules() {
      addSerializer(Instant.class, new DateTimeSerializer());
    }
  }

  public static class DateTimeSerializer extends ValueSerializer<Instant> {

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializationContext context)
        throws JacksonException {
      if (value == null) {
        gen.writeNull();
      } else {
        gen.writeString(DateTimes.formatUtc(value));
      }
    }
  }
}
