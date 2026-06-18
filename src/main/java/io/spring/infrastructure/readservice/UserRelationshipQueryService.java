package io.spring.infrastructure.readservice;

import java.util.List;
import java.util.Set;

public interface UserRelationshipQueryService {
  boolean isUserFollowing(String userId, String anotherUserId);

  Set<String> followingAuthors(String userId, List<String> ids);

  List<String> followedUsers(String userId);
}
