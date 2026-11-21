package com.example.server.domain.routine.entity;

import com.example.server.domain.image.entity.Image;
import com.example.server.global.entity.ActorAuditedEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "routine_cards", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cards_public_id", columnNames = "public_id"),
    @UniqueConstraint(name = "uk_cards_routine_order", columnNames = {"routine_id", "sort_order"})
}, indexes = {
    @Index(name = "idx_cards_image", columnList = "image_id")
}, check = {
    @CheckConstraint(name = "chk_cards_sort_order", constraint = "sort_order between 1 and 10")
})
@AttributeOverride(name = "createdBy", column = @Column(name = "created_by", nullable = false, updatable = false))
@AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by", nullable = false))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutineCard extends ActorAuditedEntity {

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "public_id", nullable = false, length = 26, updatable = false)
  private String publicId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "routine_id", nullable = false)
  private Routine routine;

  @Column(name = "sort_order", nullable = false, columnDefinition = "TINYINT UNSIGNED")
  private int sortOrder;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "image_id", nullable = false)
  private Image image;

  @Column(nullable = false, length = 30)
  private String title;

  @Column(length = 100)
  private String content;

  @Column(name = "card_time")
  private LocalTime cardTime;

  public RoutineCard(String publicId, Routine routine, int sortOrder, Image image, String title,
      String content, LocalTime cardTime) {
    super(routine.getCreatedBy());
    this.publicId = publicId;
    this.routine = routine;
    this.sortOrder = sortOrder;
    this.image = image;
    this.title = title;
    this.content = content;
    this.cardTime = cardTime;
  }
}
