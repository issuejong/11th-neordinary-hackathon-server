package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.RoutineRankingSnapshot;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RoutineRankingSnapshotRepository extends JpaRepository<RoutineRankingSnapshot, String> {

  @Modifying
  @Query("delete from RoutineRankingSnapshot s where s.expiresAt <= :now")
  void deleteExpired(Instant now);
}
