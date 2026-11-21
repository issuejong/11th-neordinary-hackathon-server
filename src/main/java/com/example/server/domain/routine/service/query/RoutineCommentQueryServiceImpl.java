package com.example.server.domain.routine.service.query;

import com.example.server.domain.routine.converter.RoutineCommentConverter;
import com.example.server.domain.routine.dto.RoutineCommentResponseDTO;
import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.repository.RoutineCommentRepository;
import com.example.server.domain.routine.repository.RoutineRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineCommentQueryServiceImpl implements RoutineCommentQueryService {

  private final RoutineRepository routineRepository;
  private final RoutineCommentRepository commentRepository;

  @Override
  public List<RoutineCommentResponseDTO> findAll(Long userId, String routineId) {
    Routine routine = routineRepository.findByPublicIdAndDeletedAtIsNull(routineId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));
    return commentRepository.findActiveViews(routine.getId()).stream()
        .map(comment -> RoutineCommentConverter.toResponse(comment, userId))
        .toList();
  }
}
