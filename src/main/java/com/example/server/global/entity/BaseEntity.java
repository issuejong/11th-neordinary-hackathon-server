package com.example.server.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, secondPrecision = 6)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, secondPrecision = 6)
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    private java.time.LocalDateTime updatedAt;
}
