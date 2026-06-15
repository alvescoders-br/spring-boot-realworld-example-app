package io.spring.api.exception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class ErrorResourceSerializer extends ValueSerializer<ErrorResource> {
  @Override
  public void serialize(ErrorResource value, JsonGenerator gen, SerializationContext context)
      throws JacksonException {
    Map<String, List<String>> json = new HashMap<>();
    gen.writeStartObject();
    gen.writeName("errors");
    gen.writeStartObject();
    for (FieldErrorResource fieldErrorResource : value.getFieldErrors()) {
      if (!json.containsKey(fieldErrorResource.getField())) {
        json.put(fieldErrorResource.getField(), new ArrayList<String>());
      }
      json.get(fieldErrorResource.getField()).add(fieldErrorResource.getMessage());
    }
    for (Map.Entry<String, List<String>> pair : json.entrySet()) {
      gen.writeName(pair.getKey());
      gen.writeStartArray();
      for (String content : pair.getValue()) {
        gen.writeString(content);
      }
      gen.writeEndArray();
    }
    gen.writeEndObject();
    gen.writeEndObject();
  }
}
