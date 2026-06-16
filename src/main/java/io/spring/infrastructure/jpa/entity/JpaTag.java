package io.spring.infrastructure.jpa.entity;

import io.spring.core.article.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
public class JpaTag {
  @Id
  @Column(length = 255)
  private String id;

  @Column(length = 255, nullable = false)
  private String name;

  protected JpaTag() {}

  private JpaTag(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public static JpaTag fromDomain(Tag tag) {
    return new JpaTag(tag.getId(), tag.getName());
  }

  public Tag toDomain() {
    Tag tag = new Tag();
    tag.setId(id);
    tag.setName(name);
    return tag;
  }
}
