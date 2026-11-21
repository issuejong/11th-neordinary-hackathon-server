package com.example.server.domain.routine.entity;

import com.example.server.domain.routine.dto.RoutineSort;
import com.example.server.global.util.Ulid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "routine_ranking_snapshots", indexes = @Index(name = "idx_ranking_expiry", columnList = "expires_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineRankingSnapshot {

  @Id
  @Column(length = 26)
  private String publicId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RoutineSort sort;

  @Column(length = 26)
  private String authorId;

  @Column(length = 26)
  private String participantId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String orderedIds;

  public RoutineRankingSnapshot(RoutineSort sort, String authorId, String participantId,
      List<String> ids, Instant expiresAt) {
    this.publicId = Ulid.generate();
    this.sort = sort;
    this.authorId = authorId;
    this.participantId = participantId;
    this.expiresAt = expiresAt;
    this.orderedIds = String.join("\n", ids);
  }

  public boolean matches(RoutineSort sort, String authorId, String participantId, Instant now) {
    return this.sort == sort && Objects.equals(this.authorId, authorId)
        && Objects.equals(this.participantId, participantId) && expiresAt.isAfter(now);
  }

  public List<String> ids() {
    return orderedIds.lines().toList();
  }
}
