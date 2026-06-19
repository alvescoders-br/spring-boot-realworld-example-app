package io.spring.application.profile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.spring.application.ProfileQueryService;
import io.spring.application.data.ProfileData;
import io.spring.application.data.UserData;
import io.spring.core.user.User;
import io.spring.infrastructure.readservice.UserReadService;
import io.spring.infrastructure.readservice.UserRelationshipQueryService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileQueryServiceUnitTest {
  @Mock private UserReadService userReadService;
  @Mock private UserRelationshipQueryService userRelationshipQueryService;

  @Test
  void should_not_check_following_when_current_user_is_absent() {
    ProfileQueryService service =
        new ProfileQueryService(userReadService, userRelationshipQueryService);
    when(userReadService.findByUsername(eq("author")))
        .thenReturn(new UserData("author-id", "author@test.com", "author", "bio", "image"));

    Optional<ProfileData> result = service.findByUsername("author", null);

    assertTrue(result.isPresent());
    assertFalse(result.get().isFollowing());
    verifyNoInteractions(userRelationshipQueryService);
  }

  @Test
  void should_mark_profile_following_when_current_user_follows_profile_user() {
    ProfileQueryService service =
        new ProfileQueryService(userReadService, userRelationshipQueryService);
    User currentUser = new User("reader@test.com", "reader", "password", "", "");
    when(userReadService.findByUsername(eq("author")))
        .thenReturn(new UserData("author-id", "author@test.com", "author", "bio", "image"));
    when(userRelationshipQueryService.isUserFollowing(eq(currentUser.getId()), eq("author-id")))
        .thenReturn(true);

    Optional<ProfileData> result = service.findByUsername("author", currentUser);

    assertTrue(result.isPresent());
    assertTrue(result.get().isFollowing());
  }
}
