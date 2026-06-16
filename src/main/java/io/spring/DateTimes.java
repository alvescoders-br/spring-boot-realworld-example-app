package io.spring;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;

public final class DateTimes {
  private static final DateTimeFormatter UTC_FORMATTER =
      new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

  private DateTimes() {}

  public static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MILLIS);
  }

  public static Instant fromEpochMillis(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis);
  }

  public static long toEpochMillis(Instant instant) {
    return instant.toEpochMilli();
  }

  public static String formatUtc(Instant instant) {
    return UTC_FORMATTER.format(instant);
  }
}
