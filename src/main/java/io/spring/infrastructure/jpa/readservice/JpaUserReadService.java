package io.spring.infrastructure.jpa.readservice;

import io.spring.application.data.UserData;
import io.spring.infrastructure.mybatis.readservice.UserReadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Primary
@Profile("postgres")
@Transactional(readOnly = true)
public class JpaUserReadService implements UserReadService {
  private final EntityManager entityManager;

  public JpaUserReadService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public UserData findByUsername(String username) {
    Query query =
        entityManager.createNativeQuery(
            "select id, email, username, bio, image from users where username = :username");
    query.setParameter("username", username);
    return singleUserData(query.getResultList());
  }

  @Override
  public UserData findById(String id) {
    Query query =
        entityManager.createNativeQuery("select id, email, username, bio, image from users where id = :id");
    query.setParameter("id", id);
    return singleUserData(query.getResultList());
  }

  private UserData singleUserData(List<Object[]> rows) {
    if (rows.isEmpty()) {
      return null;
    }
    Object[] row = rows.get(0);
    return new UserData(
        JpaReadModelDataMapper.stringValue(row[0]),
        JpaReadModelDataMapper.stringValue(row[1]),
        JpaReadModelDataMapper.stringValue(row[2]),
        JpaReadModelDataMapper.stringValue(row[3]),
        JpaReadModelDataMapper.stringValue(row[4]));
  }
}
