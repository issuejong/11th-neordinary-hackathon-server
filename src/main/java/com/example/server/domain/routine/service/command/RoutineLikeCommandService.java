package com.example.server.domain.routine.service.command;

import com.example.server.domain.routine.dto.RoutineResponseDTO.Interactions;

public interface RoutineLikeCommandService {

  Interactions like(Long userId, String routineId);

  Interactions unlike(Long userId, String routineId);
}
