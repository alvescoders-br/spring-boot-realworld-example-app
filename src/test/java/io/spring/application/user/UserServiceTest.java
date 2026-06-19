package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @Test
  void should_update_target_user_and_save_it() {
    UserService service = new UserService(userRepository, "default-image", passwordEncoder);
    User targetUser = new User("old@test.com", "old", "old-password", "old bio", "old image");
    UpdateUserParam param =
        new UpdateUserParam("new@test.com", "new-password", "new", "new bio", "new image");

    service.updateUser(new UpdateUserCommand(targetUser, param));

    assertEquals("new@test.com", targetUser.getEmail());
    assertEquals("new", targetUser.getUsername());
    assertEquals("new-password", targetUser.getPassword());
    assertEquals("new bio", targetUser.getBio());
    assertEquals("new image", targetUser.getImage());
    verify(userRepository).save(targetUser);
  }
}
