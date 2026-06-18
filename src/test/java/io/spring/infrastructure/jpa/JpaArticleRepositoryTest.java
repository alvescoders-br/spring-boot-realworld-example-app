package io.spring.infrastructure.jpa;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.article.Article;
import io.spring.core.article.Tag;
import io.spring.infrastructure.jpa.entity.JpaArticle;
import io.spring.infrastructure.jpa.entity.JpaArticleTagRelation;
import io.spring.infrastructure.jpa.entity.JpaArticleTagRelationId;
import io.spring.infrastructure.jpa.entity.JpaTag;
import io.spring.infrastructure.jpa.repository.SpringDataJpaArticleRepository;
import io.spring.infrastructure.jpa.repository.SpringDataJpaArticleTagRelationRepository;
import io.spring.infrastructure.jpa.repository.SpringDataJpaTagRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JpaArticleRepositoryTest {
  @Mock private SpringDataJpaArticleRepository springDataArticleRepository;

  @Mock private SpringDataJpaTagRepository springDataTagRepository;

  @Mock private SpringDataJpaArticleTagRelationRepository springDataArticleTagRelationRepository;

  private JpaArticleRepository articleRepository;

  @BeforeEach
  public void setUp() {
    articleRepository =
        new JpaArticleRepository(
            springDataArticleRepository, springDataTagRepository, springDataArticleTagRelationRepository);
  }

  @Test
  public void should_create_article_with_deduplicated_existing_and_new_tags() {
    Article article = article("article title", List.of("java", "spring", "java"));
    Tag existingTag = new Tag("java");
    when(springDataArticleRepository.existsById(article.getId())).thenReturn(false);
    when(springDataTagRepository.findByName("java")).thenReturn(Optional.of(JpaTag.fromDomain(existingTag)));
    when(springDataTagRepository.findByName("spring")).thenReturn(Optional.empty());
    when(springDataTagRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    articleRepository.save(article);

    verify(springDataTagRepository, never()).save(argThat(jpaTag -> jpaTag.toDomain().equals(existingTag)));
    verify(springDataTagRepository).save(argThat(jpaTag -> jpaTag.toDomain().equals(new Tag("spring"))));
    verify(springDataArticleTagRelationRepository)
        .save(
            argThat(
                relation ->
                    relation.getArticleId().equals(article.getId())
                        && relation.getTagId().equals(existingTag.getId())));
    verify(springDataArticleTagRelationRepository)
        .save(
            argThat(
                relation ->
                    relation.getArticleId().equals(article.getId())
                        && relation.getTagId().equals(tagId(article, "spring"))));
    verify(springDataArticleRepository)
        .save(
            argThat(
                jpaArticle -> {
                  Article savedArticle = jpaArticle.toDomain(article.getTags());
                  return savedArticle.equals(article)
                      && savedArticle.getSlug().equals(article.getSlug())
                      && savedArticle.getBody().equals(article.getBody());
                }));
  }

  @Test
  public void should_find_article_by_slug_with_tags() {
    Article article = article("find me", List.of("java", "spring"));
    when(springDataArticleRepository.findBySlugAndDeletedFalse(article.getSlug()))
        .thenReturn(Optional.of(JpaArticle.fromDomain(article)));
    when(springDataArticleTagRelationRepository.findByIdArticleId(article.getId()))
        .thenReturn(
            article.getTags().stream()
                .map(tag -> new JpaArticleTagRelation(article.getId(), tag.getId()))
                .toList());
    when(springDataTagRepository.findAllById(article.getTags().stream().map(Tag::getId).toList()))
        .thenReturn(article.getTags().stream().map(JpaTag::fromDomain).toList());

    Optional<Article> result = articleRepository.findBySlug(article.getSlug());

    Assertions.assertTrue(result.isPresent());
    Assertions.assertEquals(article, result.get());
    Assertions.assertTrue(result.get().getTags().contains(new Tag("java")));
    Assertions.assertTrue(result.get().getTags().contains(new Tag("spring")));
  }

  @Test
  public void should_update_only_non_empty_article_fields_and_keep_existing_tags_and_timestamps() {
    Instant createdAt = Instant.parse("2026-06-16T10:15:30.000Z");
    Instant updatedAt = Instant.parse("2026-06-16T10:15:30.000Z");
    Article existingArticle =
        Article.restored(
            "article-id",
            "user-id",
            "old-title",
            "old title",
            "old description",
            "old body",
            List.of(new Tag("java")),
            createdAt,
            updatedAt);
    Article updatedArticle =
        Article.restored(
            existingArticle.getId(),
            existingArticle.getUserId(),
            "new-title",
            "new title",
            "",
            "",
            existingArticle.getTags(),
            existingArticle.getCreatedAt(),
            Instant.parse("2026-06-16T11:00:00.000Z"));
    when(springDataArticleRepository.existsById(existingArticle.getId())).thenReturn(true);
    when(springDataArticleRepository.findByIdAndDeletedFalse(existingArticle.getId()))
        .thenReturn(Optional.of(JpaArticle.fromDomain(existingArticle)));
    when(springDataArticleTagRelationRepository.findByIdArticleId(existingArticle.getId()))
        .thenReturn(List.of(new JpaArticleTagRelation(existingArticle.getId(), tagId(existingArticle, "java"))));
    when(springDataTagRepository.findAllById(List.of(tagId(existingArticle, "java"))))
        .thenReturn(List.of(JpaTag.fromDomain(new Tag("java"))));

    articleRepository.save(updatedArticle);

    verify(springDataArticleRepository)
        .save(
            argThat(
                jpaArticle -> {
                  Article savedArticle = jpaArticle.toDomain(existingArticle.getTags());
                  return savedArticle.getTitle().equals("new title")
                      && savedArticle.getSlug().equals("new-title")
                      && savedArticle.getDescription().equals("old description")
                      && savedArticle.getBody().equals("old body")
                      && savedArticle.getUpdatedAt().equals(updatedAt);
                }));
    verify(springDataArticleTagRelationRepository, never()).save(any());
  }

  @Test
  public void should_soft_delete_article_by_id() {
    Article article = article("delete me", List.of("java"));

    articleRepository.remove(article);

    verify(springDataArticleRepository).softDeleteById(article.getId());
    verify(springDataArticleRepository, never()).deleteById(article.getId());
  }

  private Article article(String title, List<String> tags) {
    return new Article(title, "description", "body", tags, "user-id");
  }

  private String tagId(Article article, String name) {
    return article.getTags().stream()
        .filter(tag -> tag.getName().equals(name))
        .findFirst()
        .orElseThrow()
        .getId();
  }
}
