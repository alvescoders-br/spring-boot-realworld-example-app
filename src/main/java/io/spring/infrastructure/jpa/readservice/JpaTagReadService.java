package io.spring.infrastructure.jpa.readservice;

import io.spring.infrastructure.readservice.TagReadService;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaTagReadService implements TagReadService {
  private final EntityManager entityManager;

  public JpaTagReadService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public List<String> all() {
    return ((List<Object>) entityManager.createNativeQuery("select name from tags").getResultList())
        .stream().map(JpaReadModelDataMapper::stringValue).toList();
  }
}
