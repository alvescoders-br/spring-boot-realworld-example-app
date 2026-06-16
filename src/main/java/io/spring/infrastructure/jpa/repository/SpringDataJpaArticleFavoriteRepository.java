package io.spring.infrastructure.jpa.repository;

import io.spring.infrastructure.jpa.entity.JpaArticleFavorite;
import io.spring.infrastructure.jpa.entity.JpaArticleFavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaArticleFavoriteRepository
    extends JpaRepository<JpaArticleFavorite, JpaArticleFavoriteId> {}
