package com.example.server.domain.user.repository;

import com.example.server.domain.user.entity.TestAccountLease;
import com.example.server.domain.user.repository.projection.TestAccountView;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestAccountLeaseRepository extends JpaRepository<TestAccountLease, Long> {

  boolean existsBySlot(Integer slot);

  boolean existsByUser_Id(Long userId);

  @Query("""
      select new com.example.server.domain.user.repository.projection.TestAccountView(
          lease.slot, u.publicId, u.nickname, i.publicId, lease.expiresAt)
      from TestAccountLease lease join lease.user u join u.profileImage i
      where u.deletedAt is null and lease.slot between 1 and :maxSlot
      order by lease.slot asc
      """)
  List<TestAccountView> findVisibleAccounts(@Param("maxSlot") int maxSlot);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select lease from TestAccountLease lease join lease.user u
      where u.publicId = :publicId and u.deletedAt is null and lease.slot between 1 and :maxSlot
      """)
  Optional<TestAccountLease> findVisibleAccountForUpdate(@Param("publicId") String publicId, @Param("maxSlot") int maxSlot);

  @Query("""
      select u.id from TestAccountLease lease join lease.user u
      where lease.tokenHash = :tokenHash and lease.expiresAt > :now and u.deletedAt is null
      """)
  Optional<Long> findActiveUserId(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

  Optional<TestAccountLease> findByUser_PublicId(String publicId);
}
