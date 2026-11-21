package com.example.server.domain.routine.service.query;

import com.example.server.domain.routine.converter.RoutineConverter;
import com.example.server.domain.routine.dto.RoutineResponseDTO;
import com.example.server.domain.routine.repository.RoutineCardRepository;
import com.example.server.domain.routine.repository.RoutineCommentRepository;
import com.example.server.domain.routine.repository.RoutineLikeRepository;
import com.example.server.domain.routine.repository.RoutineRepository;
import com.example.server.domain.routine.repository.projection.RoutineHeaderView;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoutineQueryServiceImpl implements RoutineQueryService {

  private final RoutineRepository routineRepository;
  private final RoutineCardRepository cardRepository;
  private final RoutineLikeRepository likeRepository;
  private final RoutineCommentRepository commentRepository;

  @Override
  @Transactional(readOnly = true)
  public RoutineResponseDTO.Detail getDetail(String routineId) {
    RoutineHeaderView header = routineRepository.findHeader(routineId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));
    return RoutineConverter.toDetail(
        header,
        cardRepository.findViewsByRoutineId(header.id()),
        likeRepository.countByRoutine_IdAndDeletedAtIsNull(header.id()),
        commentRepository.countByRoutine_IdAndDeletedAtIsNull(header.id()));
  }
}
