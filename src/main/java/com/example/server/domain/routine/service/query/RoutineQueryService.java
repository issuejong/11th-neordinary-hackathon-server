package com.example.server.domain.routine.service.query;

import com.example.server.domain.routine.dto.RoutineResponseDTO;
import com.example.server.domain.routine.dto.RoutineSort;

public interface RoutineQueryService {

  RoutineResponseDTO.Detail getDetail(String routineId);

  RoutineResponseDTO.Page getList(RoutineSort sort, String cursor, int size);
}
