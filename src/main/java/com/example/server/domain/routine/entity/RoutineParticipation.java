package com.example.server.domain.routine.entity;

import com.example.server.domain.user.entity.User;
import com.example.server.global.entity.SoftDeletableEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "routine_participations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_part_routine_user", columnNames = {"routine_id", "user_id"})
}, indexes = {
    @Index(name = "idx_part_routine_status", columnList = "routine_id, status, deleted_at"),
    @Index(name = "idx_part_user", columnList = "user_id")
})
@AttributeOverride(name = "createdBy", column = @Column(name = "created_by", nullable = false, updatable = false))
@AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", nullable = false))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineParticipation extends SoftDeletableEntity {

  public enum Status {
    IN_PROGRESS,
    COMPLETED
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "routine_id", nullable = false)
  private Routine routine;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'IN_PROGRESS'")
  private Status status;

  @Column(name = "joined_at", nullable = false)
  @ColumnDefault("CURRENT_TIMESTAMP")
  private LocalDateTime joinedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  public RoutineParticipation(Routine routine, User user, LocalDateTime joinedAt) {
    super(user.getId());
    this.routine = routine;
    this.user = user;
    this.status = Status.IN_PROGRESS;
    this.joinedAt = Objects.requireNonNull(joinedAt);
  }

  public void complete(LocalDateTime completedAt) {
    if (getDeletedAt() != null || status != Status.IN_PROGRESS) {
      throw new IllegalStateException("Only an active participation in progress can be completed");
    }
    this.completedAt = Objects.requireNonNull(completedAt);
    this.status = Status.COMPLETED;
    recordUpdate(user.getId());
  }

  public void joinAgain(LocalDateTime joinedAt) {
    if (getDeletedAt() == null) {
      throw new IllegalStateException("Only a canceled participation can be restarted");
    }
    this.joinedAt = Objects.requireNonNull(joinedAt);
    this.completedAt = null;
    this.status = Status.IN_PROGRESS;
    restore(user.getId());
  }
}
