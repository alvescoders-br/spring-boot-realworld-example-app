package io.spring.infrastructure.jpa;

import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.jpa.entity.JpaFollowRelation;
import io.spring.infrastructure.jpa.entity.JpaFollowRelationId;
import io.spring.infrastructure.jpa.entity.JpaUser;
import io.spring.infrastructure.jpa.repository.SpringDataJpaFollowRelationRepository;
import io.spring.infrastructure.jpa.repository.SpringDataJpaUserRepository;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class JpaUserRepository implements UserRepository {
  private final SpringDataJpaUserRepository userRepository;
  private final SpringDataJpaFollowRelationRepository followRelationRepository;

  public JpaUserRepository(
      SpringDataJpaUserRepository userRepository,
      SpringDataJpaFollowRelationRepository followRelationRepository) {
    this.userRepository = userRepository;
    this.followRelationRepository = followRelationRepository;
  }

  @Override
  @Transactional
  public void save(User user) {
    userRepository.save(JpaUser.fromDomain(user));
  }

  @Override
  public Optional<User> findById(String id) {
    return userRepository.findById(id).map(JpaUser::toDomain);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username).map(JpaUser::toDomain);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email).map(JpaUser::toDomain);
  }

  @Override
  @Transactional
  public void saveRelation(FollowRelation followRelation) {
    JpaFollowRelationId relationId = relationId(followRelation);
    if (followRelationRepository.existsById(relationId)) {
      return;
    }
    followRelationRepository.save(JpaFollowRelation.fromDomain(followRelation));
  }

  @Override
  public Optional<FollowRelation> findRelation(String userId, String targetId) {
    return followRelationRepository
        .findById(new JpaFollowRelationId(userId, targetId))
        .map(JpaFollowRelation::toDomain);
  }

  @Override
  @Transactional
  public void removeRelation(FollowRelation followRelation) {
    followRelationRepository.deleteById(relationId(followRelation));
  }

  private JpaFollowRelationId relationId(FollowRelation followRelation) {
    return new JpaFollowRelationId(followRelation.getUserId(), followRelation.getTargetId());
  }
}
