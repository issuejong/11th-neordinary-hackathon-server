package com.example.server.domain.routine.service.command;

import com.example.server.domain.routine.dto.RoutineResponseDTO.Interactions;
import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineLike;
import com.example.server.domain.routine.repository.RoutineLikeRepository;
import com.example.server.domain.routine.repository.RoutineRepository;
import com.example.server.domain.routine.service.query.RoutineQueryService;
import com.example.server.domain.user.repository.UserRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoutineLikeCommandServiceImpl implements RoutineLikeCommandService {

  private final RoutineRepository routineRepository;
  private final RoutineLikeRepository likeRepository;
  private final UserRepository userRepository;
  private final RoutineQueryService routineQueryService;

  @Override
  public Interactions like(Long userId, String routineId) {
    Routine routine = lockRoutine(routineId);
    RoutineLike existing = likeRepository.findByRoutine_IdAndUser_Id(routine.getId(), userId).orElse(null);
    if (existing == null) {
      likeRepository.save(new RoutineLike(routine, userRepository.getReferenceById(userId)));
    } else if (existing.getDeletedAt() != null) {
      existing.likeAgain();
    }
    return routineQueryService.getInteractions(userId, routineId);
  }

  @Override
  public Interactions unlike(Long userId, String routineId) {
    Routine routine = lockRoutine(routineId);
    likeRepository.findByRoutine_IdAndUser_Id(routine.getId(), userId)
        .filter(like -> like.getDeletedAt() == null)
        .ifPresent(like -> like.markDeleted(userId, LocalDateTime.now()));
    return routineQueryService.getInteractions(userId, routineId);
  }

  private Routine lockRoutine(String routineId) {
    // 좋아요 행이 아직 없어도 같은 루틴의 요청을 순서대로 처리한다.
    return routineRepository.findActiveForUpdate(routineId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));
  }
}
