package io.spring.infrastructure.jpa;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.infrastructure.jpa.entity.JpaFollowRelation;
import io.spring.infrastructure.jpa.entity.JpaFollowRelationId;
import io.spring.infrastructure.jpa.entity.JpaUser;
import io.spring.infrastructure.jpa.repository.SpringDataJpaFollowRelationRepository;
import io.spring.infrastructure.jpa.repository.SpringDataJpaUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@ExtendWith(MockitoExtension.class)
public class JpaUserRepositoryTest {
  @Mock private SpringDataJpaUserRepository springDataUserRepository;

  @Mock private SpringDataJpaFollowRelationRepository springDataFollowRelationRepository;

  private JpaUserRepository userRepository;

  @BeforeEach
  public void setUp() {
    userRepository =
        new JpaUserRepository(springDataUserRepository, springDataFollowRelationRepository);
  }

  @Test
  public void should_save_user_with_equivalent_domain_data() {
    User user = new User("aisensiy@163.com", "aisensiy", "123", "", "default");

    userRepository.save(user);

    verify(springDataUserRepository)
        .save(
            argThat(
                jpaUser -> {
                  User savedUser = jpaUser.toDomain();
                  return savedUser.equals(user)
                      && savedUser.getEmail().equals(user.getEmail())
                      && savedUser.getUsername().equals(user.getUsername())
                      && savedUser.getPassword().equals(user.getPassword())
                      && savedUser.getBio().equals(user.getBio())
                      && savedUser.getImage().equals(user.getImage());
                }));
  }

  @Test
  public void should_find_user_by_email_for_login_flow() {
    User user = new User("john@jacob.com", "johnjacob", "encoded", "", "avatar");
    when(springDataUserRepository.findByEmail(user.getEmail()))
        .thenReturn(Optional.of(JpaUser.fromDomain(user)));

    Optional<User> result = userRepository.findByEmail(user.getEmail());

    Assertions.assertTrue(result.isPresent());
    Assertions.assertEquals(user, result.get());
    Assertions.assertEquals(user.getEmail(), result.get().getEmail());
    Assertions.assertEquals(user.getPassword(), result.get().getPassword());
  }

  @Test
  public void should_save_follow_relation_once() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");
    JpaFollowRelationId relationId = new JpaFollowRelationId("user-id", "target-id");
    when(springDataFollowRelationRepository.existsById(relationId)).thenReturn(false);

    userRepository.saveRelation(relation);

    verify(springDataFollowRelationRepository)
        .save(
            argThat(
                jpaRelation -> {
                  FollowRelation savedRelation = jpaRelation.toDomain();
                  return savedRelation.equals(relation);
                }));
  }

  @Test
  public void should_not_duplicate_existing_follow_relation() {
    FollowRelation relation = new FollowRelation("user-id", "target-id");
    JpaFollowRelationId relationId = new JpaFollowRelationId("user-id", "target-id");
    when(springDataFollowRelationRepository.existsById(relationId)).thenReturn(true);

    userRepository.saveRelation(relation);

    verify(springDataFollowRelationRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void should_find_and_remove_follow_relation() {
    JpaFollowRelationId relationId = new JpaFollowRelationId("user-id", "target-id");
    FollowRelation relation = new FollowRelation("user-id", "target-id");
    when(springDataFollowRelationRepository.findById(relationId))
        .thenReturn(Optional.of(JpaFollowRelation.fromDomain(relation)));

    Optional<FollowRelation> result = userRepository.findRelation("user-id", "target-id");
    userRepository.removeRelation(relation);

    Assertions.assertTrue(result.isPresent());
    Assertions.assertEquals(relation, result.get());
    verify(springDataFollowRelationRepository).deleteById(relationId);
  }

  @Test
  public void should_execute_all_user_operations_through_spring_context() {
    try (AnnotationConfigApplicationContext context = buildContext()) {
      UserRepository repository = context.getBean(UserRepository.class);
      SpringDataJpaUserRepository springDataUsers =
          context.getBean(SpringDataJpaUserRepository.class);
      SpringDataJpaFollowRelationRepository springDataFollowRelations =
          context.getBean(SpringDataJpaFollowRelationRepository.class);
      User user = new User("spring-context@example.com", "spring-context", "encoded", "bio", "image");
      FollowRelation relation = new FollowRelation(user.getId(), "followed-id");
      JpaFollowRelationId relationId = new JpaFollowRelationId(user.getId(), "followed-id");

      when(springDataUsers.findById(user.getId())).thenReturn(Optional.of(JpaUser.fromDomain(user)));
      when(springDataUsers.findByUsername(user.getUsername()))
          .thenReturn(Optional.of(JpaUser.fromDomain(user)));
      when(springDataUsers.findByEmail(user.getEmail()))
          .thenReturn(Optional.of(JpaUser.fromDomain(user)));
      when(springDataFollowRelations.existsById(relationId)).thenReturn(false);
      when(springDataFollowRelations.findById(relationId))
          .thenReturn(Optional.of(JpaFollowRelation.fromDomain(relation)));

      repository.save(user);
      Optional<User> userById = repository.findById(user.getId());
      Optional<User> userByUsername = repository.findByUsername(user.getUsername());
      Optional<User> userByEmail = repository.findByEmail(user.getEmail());
      repository.saveRelation(relation);
      Optional<FollowRelation> followRelation =
          repository.findRelation(relation.getUserId(), relation.getTargetId());
      repository.removeRelation(relation);

      Assertions.assertEquals(Optional.of(user), userById);
      Assertions.assertEquals(Optional.of(user), userByUsername);
      Assertions.assertEquals(Optional.of(user), userByEmail);
      Assertions.assertEquals(Optional.of(relation), followRelation);
      verify(springDataUsers).save(argThat(jpaUser -> jpaUser.toDomain().equals(user)));
      verify(springDataFollowRelations)
          .save(argThat(jpaRelation -> jpaRelation.toDomain().equals(relation)));
      verify(springDataFollowRelations).deleteById(relationId);
    }
  }

  private AnnotationConfigApplicationContext buildContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(
        SpringDataJpaUserRepository.class, () -> mock(SpringDataJpaUserRepository.class));
    context.registerBean(
        SpringDataJpaFollowRelationRepository.class,
        () -> mock(SpringDataJpaFollowRelationRepository.class));
    context.register(JpaUserRepository.class);
    context.refresh();
    return context;
  }
}
