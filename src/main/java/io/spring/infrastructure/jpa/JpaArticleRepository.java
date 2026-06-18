package io.spring.infrastructure.jpa;

import io.spring.Util;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.article.Tag;
import io.spring.infrastructure.jpa.entity.JpaArticle;
import io.spring.infrastructure.jpa.entity.JpaArticleTagRelation;
import io.spring.infrastructure.jpa.entity.JpaArticleTagRelationId;
import io.spring.infrastructure.jpa.entity.JpaTag;
import io.spring.infrastructure.jpa.repository.SpringDataJpaArticleRepository;
import io.spring.infrastructure.jpa.repository.SpringDataJpaArticleTagRelationRepository;
import io.spring.infrastructure.jpa.repository.SpringDataJpaTagRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaArticleRepository implements ArticleRepository {
  private final SpringDataJpaArticleRepository articleRepository;
  private final SpringDataJpaTagRepository tagRepository;
  private final SpringDataJpaArticleTagRelationRepository articleTagRelationRepository;

  public JpaArticleRepository(
      SpringDataJpaArticleRepository articleRepository,
      SpringDataJpaTagRepository tagRepository,
      SpringDataJpaArticleTagRelationRepository articleTagRelationRepository) {
    this.articleRepository = articleRepository;
    this.tagRepository = tagRepository;
    this.articleTagRelationRepository = articleTagRelationRepository;
  }

  @Override
  @Transactional
  public void save(Article article) {
    if (articleRepository.existsById(article.getId())) {
      updateExisting(article);
      return;
    }
    createNew(article);
  }

  @Override
  public Optional<Article> findById(String id) {
    return articleRepository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Article> findBySlug(String slug) {
    return articleRepository.findBySlug(slug).map(this::toDomain);
  }

  @Override
  @Transactional
  public void remove(Article article) {
    articleRepository.deleteById(article.getId());
  }

  private void createNew(Article article) {
    for (Tag tag : article.getTags()) {
      JpaTag targetTag = tagRepository.findByName(tag.getName()).orElseGet(() -> saveTag(tag));
      saveArticleTagRelation(article.getId(), targetTag.toDomain().getId());
    }
    articleRepository.save(JpaArticle.fromDomain(article));
  }

  private JpaTag saveTag(Tag tag) {
    return tagRepository.save(JpaTag.fromDomain(tag));
  }

  private void saveArticleTagRelation(String articleId, String tagId) {
    JpaArticleTagRelationId relationId = new JpaArticleTagRelationId(articleId, tagId);
    if (articleTagRelationRepository.existsById(relationId)) {
      return;
    }
    articleTagRelationRepository.save(new JpaArticleTagRelation(articleId, tagId));
  }

  private void updateExisting(Article article) {
    Article existingArticle = findById(article.getId()).orElseThrow();
    Article mergedArticle =
        Article.restored(
            existingArticle.getId(),
            existingArticle.getUserId(),
            Util.isEmpty(article.getTitle()) ? existingArticle.getSlug() : article.getSlug(),
            Util.isEmpty(article.getTitle()) ? existingArticle.getTitle() : article.getTitle(),
            Util.isEmpty(article.getDescription())
                ? existingArticle.getDescription()
                : article.getDescription(),
            Util.isEmpty(article.getBody()) ? existingArticle.getBody() : article.getBody(),
            existingArticle.getTags(),
            existingArticle.getCreatedAt(),
            existingArticle.getUpdatedAt());
    articleRepository.save(JpaArticle.fromDomain(mergedArticle));
  }

  private Article toDomain(JpaArticle article) {
    List<String> tagIds =
        articleTagRelationRepository.findByIdArticleId(article.getId()).stream()
            .map(JpaArticleTagRelation::getTagId)
            .toList();
    List<Tag> tags = tagRepository.findAllById(tagIds).stream().map(JpaTag::toDomain).toList();
    return article.toDomain(tags);
  }
}
