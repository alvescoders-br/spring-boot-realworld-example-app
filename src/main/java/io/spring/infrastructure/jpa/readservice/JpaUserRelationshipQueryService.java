package io.spring.infrastructure.jpa.readservice;

import io.spring.infrastructure.mybatis.readservice.UserRelationshipQueryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Primary
@Profile("postgres")
@Transactional(readOnly = true)
public class JpaUserRelationshipQueryService implements UserRelationshipQueryService {
  private final EntityManager entityManager;

  public JpaUserRelationshipQueryService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public boolean isUserFollowing(String userId, String anotherUserId) {
    Query query =
        entityManager.createNativeQuery(
            "select count(1) from follows where user_id = :userId and follow_id = :anotherUserId");
    query.setParameter("userId", userId);
    query.setParameter("anotherUserId", anotherUserId);
    return ((Number) query.getSingleResult()).intValue() > 0;
  }

  @Override
  public Set<String> followingAuthors(String userId, List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return Set.of();
    }
    Query query =
        entityManager.createNativeQuery(
            "select F.follow_id from follows F where F.follow_id in (:ids) and F.user_id = :userId");
    query.setParameter("ids", ids);
    query.setParameter("userId", userId);
    return ((List<Object>) query.getResultList())
        .stream().map(JpaReadModelDataMapper::stringValue).collect(Collectors.toSet());
  }

  @Override
  public List<String> followedUsers(String userId) {
    Query query =
        entityManager.createNativeQuery("select F.follow_id from follows F where F.user_id = :userId");
    query.setParameter("userId", userId);
    return ((List<Object>) query.getResultList())
        .stream().map(JpaReadModelDataMapper::stringValue).toList();
  }
}
