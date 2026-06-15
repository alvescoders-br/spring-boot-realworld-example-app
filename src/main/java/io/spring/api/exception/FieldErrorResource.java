package io.spring.api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FieldErrorResource {
  private String resource;
  private String field;
  private String code;
  private String message;
}
