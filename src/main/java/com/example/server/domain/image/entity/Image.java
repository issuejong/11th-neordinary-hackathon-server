package com.example.server.domain.image.entity;

import com.example.server.global.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "images", uniqueConstraints = {
    @UniqueConstraint(name = "uk_images_public_id", columnNames = "public_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends SoftDeletableEntity {

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "public_id", nullable = false, length = 26, updatable = false)
  private String publicId;

  @Column(name = "content_type", nullable = false, length = 50)
  private String contentType;

  @Column(name = "byte_size", nullable = false, columnDefinition = "INT UNSIGNED")
  private int byteSize;

  @Lob
  @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
  private byte[] data;

  public Image(String publicId, String contentType, byte[] data, Long actorId) {
    super(actorId);
    this.publicId = publicId;
    this.contentType = contentType;
    this.byteSize = data.length;
    this.data = data;
  }
}
