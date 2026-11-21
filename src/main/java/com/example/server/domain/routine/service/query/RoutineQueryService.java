package com.example.server.domain.routine.service.query;

import com.example.server.domain.routine.dto.RoutineResponseDTO;

public interface RoutineQueryService {

  RoutineResponseDTO.Detail getDetail(String routineId);
}
