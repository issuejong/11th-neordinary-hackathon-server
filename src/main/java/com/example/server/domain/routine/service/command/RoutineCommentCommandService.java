package com.example.server.domain.routine.service.command;

import com.example.server.domain.routine.dto.RoutineCommentRequestDTO.CommentBody;
import com.example.server.domain.routine.dto.RoutineCommentResponseDTO;

public interface RoutineCommentCommandService {

  RoutineCommentResponseDTO create(Long userId, String routineId, CommentBody request);

  RoutineCommentResponseDTO update(Long userId, String routineId, String commentId, CommentBody request);

  void delete(Long userId, String routineId, String commentId);
}
