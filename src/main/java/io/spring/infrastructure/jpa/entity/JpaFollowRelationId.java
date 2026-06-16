package io.spring.infrastructure.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class JpaFollowRelationId implements Serializable {
  @Column(name = "user_id", length = 255, nullable = false)
  private String userId;

  @Column(name = "follow_id", length = 255, nullable = false)
  private String targetId;

  protected JpaFollowRelationId() {}

  public JpaFollowRelationId(String userId, String targetId) {
    this.userId = userId;
    this.targetId = targetId;
  }

  public String getUserId() {
    return userId;
  }

  public String getTargetId() {
    return targetId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof JpaFollowRelationId that)) {
      return false;
    }
    return Objects.equals(userId, that.userId) && Objects.equals(targetId, that.targetId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, targetId);
  }
}
