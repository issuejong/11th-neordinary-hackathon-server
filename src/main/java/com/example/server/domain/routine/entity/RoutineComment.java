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
@Table(name = "routine_comments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_comments_public_id", columnNames = "public_id")
}, indexes = {
    @Index(name = "idx_comments_routine", columnList = "routine_id, deleted_at, id"),
    @Index(name = "idx_comments_user", columnList = "user_id")
})
@AttributeOverride(name = "createdBy", column = @Column(name = "created_by", nullable = false, updatable = false))
@AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", nullable = false))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineComment extends SoftDeletableEntity {

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "public_id", nullable = false, length = 26, updatable = false)
  private String publicId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "routine_id", nullable = false)
  private Routine routine;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, length = 50)
  private String content;

  public RoutineComment(String publicId, Routine routine, User user, String content) {
    super(user.getId());
    this.publicId = publicId;
    this.routine = routine;
    this.user = user;
    this.content = content;
  }

  public void edit(String content) {
    this.content = content;
    recordUpdate(user.getId());
  }
}
