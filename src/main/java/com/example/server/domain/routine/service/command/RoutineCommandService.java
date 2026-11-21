package com.example.server.domain.routine.service.command;

import com.example.server.domain.routine.dto.RoutineRequestDTO;
import com.example.server.domain.routine.dto.RoutineResponseDTO;

public interface RoutineCommandService {

  RoutineResponseDTO.Detail create(Long userId, RoutineRequestDTO.Save request);

  RoutineResponseDTO.Detail update(Long userId, String routineId, RoutineRequestDTO.Save request);

  void delete(Long userId, String routineId);
}
