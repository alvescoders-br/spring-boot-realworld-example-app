package io.spring.infrastructure.jpa.entity;

import io.spring.core.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class JpaUser {
  @Id
  @Column(length = 255)
  private String id;

  @Column(length = 255, unique = true)
  private String username;

  @Column(length = 255, unique = true)
  private String email;

  @Column(length = 255)
  private String password;

  @Column(columnDefinition = "text")
  private String bio;

  @Column(length = 511)
  private String image;

  protected JpaUser() {}

  private JpaUser(String id, String email, String username, String password, String bio, String image) {
    this.id = id;
    this.email = email;
    this.username = username;
    this.password = password;
    this.bio = bio;
    this.image = image;
  }

  public static JpaUser fromDomain(User user) {
    return new JpaUser(
        user.getId(),
        user.getEmail(),
        user.getUsername(),
        user.getPassword(),
        user.getBio(),
        user.getImage());
  }

  public User toDomain() {
    return User.restored(id, email, username, password, bio, image);
  }
}
