package com.example.server.domain.user.entity;

import com.example.server.domain.image.entity.Image;
import com.example.server.global.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "AppUser")
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_users_public_id", columnNames = "public_id"),
    @UniqueConstraint(name = "uk_users_nickname_key", columnNames = "nickname_key")
}, indexes = {
    @Index(name = "idx_users_profile_image", columnList = "profile_image_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletableEntity {

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "public_id", nullable = false, length = 26, updatable = false)
  private String publicId;

  @Column(nullable = false, length = 30)
  private String nickname;

  @Column(name = "nickname_key", length = 60)
  private String nicknameKey;

  @Column(length = 100)
  private String bio;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "profile_image_id", nullable = false)
  private Image profileImage;

  public User(String publicId, String nickname, Image profileImage) {
    this(publicId, nickname, profileImage, null);
  }

  public User(String publicId, String nickname, Image profileImage, String bio) {
    this.publicId = publicId;
    this.nickname = nickname;
    this.profileImage = profileImage;
    this.bio = bio;
    this.nicknameKey = nickname.toLowerCase(Locale.ROOT);
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
    this.nicknameKey = nickname.toLowerCase(Locale.ROOT);
    recordUpdate(getId());
  }

  public void changeBio(String bio) {
    this.bio = bio;
    recordUpdate(getId());
  }

  public void changeProfileImage(Image profileImage) {
    this.profileImage = profileImage;
    recordUpdate(getId());
  }
}
