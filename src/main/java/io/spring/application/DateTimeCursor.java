package io.spring.application;

import io.spring.DateTimes;
import java.time.Instant;

public class DateTimeCursor extends PageCursor<Instant> {

  public DateTimeCursor(Instant data) {
    super(data);
  }

  @Override
  public String toString() {
    return String.valueOf(DateTimes.toEpochMillis(getData()));
  }

  public static Instant parse(String cursor) {
    if (cursor == null) {
      return null;
    }
    return DateTimes.fromEpochMillis(Long.parseLong(cursor));
  }
}
