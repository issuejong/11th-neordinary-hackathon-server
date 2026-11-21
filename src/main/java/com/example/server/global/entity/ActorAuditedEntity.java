package com.example.server.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ActorAuditedEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "created_by", updatable = false)
  private Long createdBy;

  @Column(name = "updated_by")
  private Long updatedBy;

  protected ActorAuditedEntity(Long actorId) {
    this.createdBy = actorId;
    this.updatedBy = actorId;
  }

  protected void recordUpdate(Long actorId) {
    this.updatedBy = actorId;
  }
}
