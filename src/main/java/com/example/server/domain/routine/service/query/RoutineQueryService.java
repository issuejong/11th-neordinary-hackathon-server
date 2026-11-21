package com.example.server.domain.routine.service.query;

import com.example.server.domain.routine.dto.RoutineResponseDTO;
import com.example.server.domain.routine.dto.RoutineSort;

public interface RoutineQueryService {

  RoutineResponseDTO.Detail getDetail(String routineId);

  RoutineResponseDTO.Interactions getInteractions(Long userId, String routineId);

  RoutineResponseDTO.Page getList(RoutineSort sort, String authorId, String participantId, String cursor, int size, Long viewerId);
}
