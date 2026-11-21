package com.example.server.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SoftDeletableEntity extends ActorAuditedEntity {

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "deleted_by")
  private Long deletedBy;

  protected SoftDeletableEntity(Long actorId) {
    super(actorId);
  }

  public void markDeleted(Long actorId, LocalDateTime deletedAt) {
    this.deletedAt = Objects.requireNonNull(deletedAt);
    this.deletedBy = actorId;
    recordUpdate(actorId);
  }

  protected void restore(Long actorId) {
    this.deletedAt = null;
    this.deletedBy = null;
    recordUpdate(actorId);
  }
}
