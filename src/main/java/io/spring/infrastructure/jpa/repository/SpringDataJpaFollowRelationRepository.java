package io.spring.infrastructure.jpa.repository;

import io.spring.infrastructure.jpa.entity.JpaFollowRelation;
import io.spring.infrastructure.jpa.entity.JpaFollowRelationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataJpaFollowRelationRepository
    extends JpaRepository<JpaFollowRelation, JpaFollowRelationId> {}
