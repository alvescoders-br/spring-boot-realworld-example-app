package io.spring.application.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class ArticleDataTest {

  @Test
  void shouldCalculateZeroReadingTimeForBlankBody() {
    assertThat(articleWithBody(null).getReadingTime()).isZero();
    assertThat(articleWithBody("").getReadingTime()).isZero();
    assertThat(articleWithBody("   ").getReadingTime()).isZero();
  }

  @Test
  void shouldRoundReadingTimeUpWithMinimumOneForNonBlankBody() {
    assertThat(articleWithBody(words(1)).getReadingTime()).isEqualTo(1);
    assertThat(articleWithBody(words(200)).getReadingTime()).isEqualTo(1);
    assertThat(articleWithBody(words(201)).getReadingTime()).isEqualTo(2);
    assertThat(articleWithBody(words(400)).getReadingTime()).isEqualTo(2);
    assertThat(articleWithBody(words(401)).getReadingTime()).isEqualTo(3);
  }

  @Test
  void shouldPreferCachedReadingTimeWhenPresent() {
    ArticleData articleData = articleWithBody(words(401));
    articleData.setCachedReadingTime(7);

    assertThat(articleData.getReadingTime()).isEqualTo(7);
  }

  private ArticleData articleWithBody(String body) {
    ArticleData articleData = new ArticleData();
    articleData.setBody(body);
    return articleData;
  }

  private String words(int wordCount) {
    return String.join(" ", Collections.nCopies(wordCount, "word"));
  }
}
