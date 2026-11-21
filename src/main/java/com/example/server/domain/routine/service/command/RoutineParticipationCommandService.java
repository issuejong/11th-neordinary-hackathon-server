package com.example.server.domain.routine.service.command;

import com.example.server.domain.routine.dto.RoutineResponseDTO.Interactions;

public interface RoutineParticipationCommandService {

  Interactions join(Long userId, String routineId);

  Interactions cancel(Long userId, String routineId);

  Interactions complete(Long userId, String routineId);
}
