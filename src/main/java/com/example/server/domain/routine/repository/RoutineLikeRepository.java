package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.RoutineLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineLikeRepository extends JpaRepository<RoutineLike, Long> {

  long countByRoutine_IdAndDeletedAtIsNull(Long routineId);
}
