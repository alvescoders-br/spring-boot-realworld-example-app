package io.spring.application;

import java.util.List;
import lombok.Getter;

@Getter
public class CursorPager<T extends Node> {
  private List<T> data;
  private boolean next;
  private boolean previous;

  public CursorPager(List<T> data, Direction direction, boolean hasExtra) {
    this(data, direction, hasExtra, false);
  }

  public CursorPager(List<T> data, Direction direction, boolean hasExtra, boolean hasCursor) {
    this.data = data;

    if (direction == Direction.NEXT) {
      this.previous = hasCursor;
      this.next = hasExtra;
      return;
    }

    this.next = hasCursor;
    this.previous = hasExtra;
  }

  public boolean hasNext() {
    return next;
  }

  public boolean hasPrevious() {
    return previous;
  }

  public PageCursor getStartCursor() {
    return data.isEmpty() ? null : data.get(0).getCursor();
  }

  public PageCursor getEndCursor() {
    return data.isEmpty() ? null : data.get(data.size() - 1).getCursor();
  }

  public enum Direction {
    PREV,
    NEXT
  }
}
