package com.example.server.domain.routine.repository;

import com.example.server.domain.routine.entity.RoutineParticipation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineParticipationRepository extends JpaRepository<RoutineParticipation, Long> {

  Optional<RoutineParticipation> findByRoutine_IdAndUser_Id(Long routineId, Long userId);
}
