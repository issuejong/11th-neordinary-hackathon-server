package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.RoutineComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineCommentRepository extends JpaRepository<RoutineComment, Long> {

  long countByRoutine_IdAndDeletedAtIsNull(Long routineId);
}
