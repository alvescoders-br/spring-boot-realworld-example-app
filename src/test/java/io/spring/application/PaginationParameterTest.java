package io.spring.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.spring.application.CursorPager.Direction;
import org.junit.jupiter.api.Test;

class PaginationParameterTest {

  @Test
  void should_keep_default_page_values_for_non_positive_inputs() {
    Page page = new Page(-1, 0);

    assertEquals(0, page.getOffset());
    assertEquals(20, page.getLimit());
  }

  @Test
  void should_apply_positive_page_values_and_cap_large_limit() {
    Page oneBasedPage = new Page(1, 1);
    Page customPage = new Page(5, 30);
    Page cappedPage = new Page(1, 101);

    assertEquals(1, oneBasedPage.getOffset());
    assertEquals(1, oneBasedPage.getLimit());
    assertEquals(5, customPage.getOffset());
    assertEquals(30, customPage.getLimit());
    assertEquals(1, cappedPage.getOffset());
    assertEquals(100, cappedPage.getLimit());
  }

  @Test
  void should_keep_default_cursor_limit_for_non_positive_input() {
    CursorPageParameter<String> page = new CursorPageParameter<>("cursor", 0, Direction.NEXT);

    assertEquals("cursor", page.getCursor());
    assertEquals(Direction.NEXT, page.getDirection());
    assertEquals(20, page.getLimit());
    assertEquals(21, page.getQueryLimit());
  }

  @Test
  void should_apply_cursor_limit_boundary_and_include_extra_row_in_query_limit() {
    CursorPageParameter<String> boundaryPage = new CursorPageParameter<>("cursor", 1000, Direction.NEXT);
    CursorPageParameter<String> cappedPage = new CursorPageParameter<>("cursor", 1001, Direction.PREV);

    assertEquals(1000, boundaryPage.getLimit());
    assertEquals(1001, boundaryPage.getQueryLimit());
    assertEquals(1000, cappedPage.getLimit());
    assertEquals(1001, cappedPage.getQueryLimit());
  }

  @Test
  void should_render_page_cursor_data_as_string() {
    PageCursor<Integer> cursor = new PageCursor<>(42) {};

    assertEquals(42, cursor.getData());
    assertEquals("42", cursor.toString());
  }
}
