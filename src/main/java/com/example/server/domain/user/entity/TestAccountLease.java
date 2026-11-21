package com.example.server.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "test_account_leases", uniqueConstraints = {
    @UniqueConstraint(name = "uk_test_account_leases_slot", columnNames = "slot"),
    @UniqueConstraint(name = "uk_test_account_leases_user_id", columnNames = "user_id"),
    @UniqueConstraint(name = "uk_test_account_leases_token_hash", columnNames = "token_hash")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestAccountLease {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Integer slot;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  @Column(name = "token_hash", length = 64)
  private String tokenHash;

  @Column(name = "expires_at")
  private Instant expiresAt;

  public TestAccountLease(int slot, User user) {
    this.slot = slot;
    this.user = user;
  }

  public boolean isActiveAt(Instant now) {
    return expiresAt != null && expiresAt.isAfter(now);
  }

  public void claim(String tokenHash, Instant expiresAt) {
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
  }

  public void renew(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public void release() {
    this.tokenHash = null;
    this.expiresAt = null;
  }
}
