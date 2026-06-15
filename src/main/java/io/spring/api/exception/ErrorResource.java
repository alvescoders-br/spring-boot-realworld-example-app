package io.spring.api.exception;

import java.util.List;
import tools.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = ErrorResourceSerializer.class)
@lombok.Getter
public class ErrorResource {
  private List<FieldErrorResource> fieldErrors;

  public ErrorResource(List<FieldErrorResource> fieldErrorResources) {
    this.fieldErrors = fieldErrorResources;
  }
}
