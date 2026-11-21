package com.example.server.domain.routine.entity;

import com.example.server.domain.user.entity.User;
import com.example.server.global.entity.SoftDeletableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "routines", uniqueConstraints = {
    @UniqueConstraint(name = "uk_routines_public_id", columnNames = "public_id")
}, indexes = {
    @Index(name = "idx_routines_owner", columnList = "user_id, deleted_at, id")
})
@AttributeOverride(name = "createdBy", column = @Column(name = "created_by", nullable = false, updatable = false))
@AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", nullable = false))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Routine extends SoftDeletableEntity {

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "public_id", nullable = false, length = 26, updatable = false)
  private String publicId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 30)
  private String title;

  @Column(length = 500)
  private String content;

  public Routine(String publicId, User user, String title, String content) {
    super(user.getId());
    this.publicId = publicId;
    this.user = user;
    this.title = title;
    this.content = content;
  }

  public void update(String title, String content, Long actorId) {
    this.title = title;
    this.content = content;
    recordUpdate(actorId);
  }

  public boolean isOwnedBy(Long userId) {
    return user.getId().equals(userId);
  }

  /** 소프트 삭제 취소 (Swagger 데모 데이터 자동 복구용) */
  public void restoreDeleted(Long actorId) {
    restore(actorId);
  }
}
