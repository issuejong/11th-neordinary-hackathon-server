package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.RoutineLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineLikeRepository extends JpaRepository<RoutineLike, Long> {

  long countByRoutine_IdAndDeletedAtIsNull(Long routineId);

  Optional<RoutineLike> findByRoutine_IdAndUser_Id(Long routineId, Long userId);
}
