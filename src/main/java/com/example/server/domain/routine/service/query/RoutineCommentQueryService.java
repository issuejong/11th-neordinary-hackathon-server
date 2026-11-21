package com.example.server.domain.routine.service.query;

import com.example.server.domain.routine.dto.RoutineCommentResponseDTO;
import java.util.List;

public interface RoutineCommentQueryService {

  List<RoutineCommentResponseDTO> findAll(Long userId, String routineId);
}
