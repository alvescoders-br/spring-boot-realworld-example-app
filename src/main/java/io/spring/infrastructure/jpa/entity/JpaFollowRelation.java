package io.spring.infrastructure.jpa.entity;

import io.spring.core.user.FollowRelation;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "follows")
public class JpaFollowRelation {
  @EmbeddedId private JpaFollowRelationId id;

  protected JpaFollowRelation() {}

  private JpaFollowRelation(JpaFollowRelationId id) {
    this.id = id;
  }

  public static JpaFollowRelation fromDomain(FollowRelation followRelation) {
    return new JpaFollowRelation(
        new JpaFollowRelationId(followRelation.getUserId(), followRelation.getTargetId()));
  }

  public FollowRelation toDomain() {
    return new FollowRelation(id.getUserId(), id.getTargetId());
  }
}
