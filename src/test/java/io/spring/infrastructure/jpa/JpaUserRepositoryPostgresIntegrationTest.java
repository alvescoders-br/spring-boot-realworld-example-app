package io.spring.infrastructure.jpa;

import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("postgres")
@Transactional
@EnabledIfEnvironmentVariable(named = "REALWORLD_POSTGRES_URL", matches = "jdbc:postgresql:.*")
public class JpaUserRepositoryPostgresIntegrationTest {
  @Autowired private UserRepository userRepository;

  @Test
  public void should_validate_schema_and_execute_user_repository_against_postgres_profile() {
    Assertions.assertInstanceOf(JpaUserRepository.class, userRepository);

    String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    User user =
        new User(
            "jpa-" + uniqueSuffix + "@example.com",
            "jpa-" + uniqueSuffix,
            "encoded-password",
            "schema validation bio",
            "https://example.com/avatar-" + uniqueSuffix + ".png");
    User followedUser =
        new User(
            "followed-" + uniqueSuffix + "@example.com",
            "followed-" + uniqueSuffix,
            "encoded-password",
            "followed bio",
            "https://example.com/followed-" + uniqueSuffix + ".png");
    FollowRelation relation = new FollowRelation(user.getId(), followedUser.getId());

    userRepository.save(user);
    userRepository.save(followedUser);
    Optional<User> userById = userRepository.findById(user.getId());
    Optional<User> userByUsername = userRepository.findByUsername(user.getUsername());
    Optional<User> userByEmail = userRepository.findByEmail(user.getEmail());

    userRepository.saveRelation(relation);
    Optional<FollowRelation> followRelation =
        userRepository.findRelation(relation.getUserId(), relation.getTargetId());
    userRepository.removeRelation(relation);
    Optional<FollowRelation> removedRelation =
        userRepository.findRelation(relation.getUserId(), relation.getTargetId());

    Assertions.assertEquals(Optional.of(user), userById);
    Assertions.assertEquals(Optional.of(user), userByUsername);
    Assertions.assertEquals(Optional.of(user), userByEmail);
    Assertions.assertEquals(Optional.of(relation), followRelation);
    Assertions.assertTrue(removedRelation.isEmpty());
  }
}
