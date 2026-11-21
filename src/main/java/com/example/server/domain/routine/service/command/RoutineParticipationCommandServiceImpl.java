package com.example.server.domain.routine.service.command;

import com.example.server.domain.routine.dto.RoutineResponseDTO.Interactions;
import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineParticipation;
import com.example.server.domain.routine.repository.RoutineParticipationRepository;
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
public class RoutineParticipationCommandServiceImpl implements RoutineParticipationCommandService {

  private final RoutineRepository routineRepository;
  private final RoutineParticipationRepository participationRepository;
  private final UserRepository userRepository;
  private final RoutineQueryService routineQueryService;

  @Override
  public Interactions join(Long userId, String routineId) {
    Routine routine = lockParticipableRoutine(userId, routineId);
    RoutineParticipation existing = participationRepository.findByRoutine_IdAndUser_Id(routine.getId(), userId)
        .orElse(null);
    if (existing == null) {
      participationRepository.save(new RoutineParticipation(routine, userRepository.getReferenceById(userId), LocalDateTime.now()));
    } else if (existing.getDeletedAt() != null) {
      existing.joinAgain(LocalDateTime.now());
    }
    return routineQueryService.getInteractions(userId, routineId);
  }

  @Override
  public Interactions cancel(Long userId, String routineId) {
    Routine routine = lockParticipableRoutine(userId, routineId);
    participationRepository.findByRoutine_IdAndUser_Id(routine.getId(), userId)
        .filter(participation -> participation.getDeletedAt() == null)
        .ifPresent(participation -> {
          if (participation.hasPublishedWork()) {
            throw new BusinessException(ErrorStatus.PARTICIPATION_INVALID_STATE);
          }
          participation.discard(LocalDateTime.now());
        });
    return routineQueryService.getInteractions(userId, routineId);
  }

  @Override
  public Interactions complete(Long userId, String routineId) {
    Routine routine = lockParticipableRoutine(userId, routineId);
    RoutineParticipation participation = participationRepository.findByRoutine_IdAndUser_Id(routine.getId(), userId)
        .filter(existing -> existing.getDeletedAt() == null)
        .orElseThrow(() -> new BusinessException(ErrorStatus.PARTICIPATION_INVALID_STATE));
    if (!participation.hasPublishedWork()) {
      throw new BusinessException(ErrorStatus.PARTICIPATION_WORK_REQUIRED);
    }
    return routineQueryService.getInteractions(userId, routineId);
  }

  private Routine lockParticipableRoutine(Long userId, String routineId) {
    Routine routine = routineRepository.findActiveForUpdate(routineId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));
    if (routine.isOwnedBy(userId)) {
      throw new BusinessException(ErrorStatus.PARTICIPATION_OWNER_FORBIDDEN);
    }
    return routine;
  }
}
