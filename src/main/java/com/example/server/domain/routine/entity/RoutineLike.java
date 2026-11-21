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

@Entity
@Table(name = "routine_likes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_likes_routine_user", columnNames = {"routine_id", "user_id"})
}, indexes = {
    @Index(name = "idx_likes_routine_active", columnList = "routine_id, deleted_at"),
    @Index(name = "idx_likes_user", columnList = "user_id")
})
@AttributeOverride(name = "createdBy", column = @Column(name = "created_by", nullable = false, updatable = false))
@AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", nullable = false))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineLike extends SoftDeletableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "routine_id", nullable = false)
  private Routine routine;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  public RoutineLike(Routine routine, User user) {
    super(user.getId());
    this.routine = routine;
    this.user = user;
  }

  public void likeAgain() {
    restore(user.getId());
  }
}
