package com.example.server.domain.routine.service;

import com.example.server.domain.image.converter.ImageConverter;
import com.example.server.domain.image.repository.ImageRepository;
import com.example.server.domain.image.support.ProfileImagePolicy;
import com.example.server.domain.routine.dto.ParticipationWorkDTO.Detail;
import com.example.server.domain.routine.dto.ParticipationWorkDTO.Draft;
import com.example.server.domain.routine.dto.ParticipationWorkDTO.DraftCard;
import com.example.server.domain.routine.dto.ParticipationWorkDTO.Page;
import com.example.server.domain.routine.dto.ParticipationWorkDTO.Summary;
import com.example.server.domain.routine.dto.RoutineRequestDTO.Save;
import com.example.server.domain.routine.dto.RoutineResponseDTO.Owner;
import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineParticipation;
import com.example.server.domain.routine.entity.RoutineParticipation.Status;
import com.example.server.domain.routine.repository.RoutineParticipationRepository;
import com.example.server.domain.routine.repository.RoutineRepository;
import com.example.server.domain.routine.service.query.RoutineQueryService;
import com.example.server.domain.routine.dto.RoutineResponseDTO.Interactions;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationWorkService {

  private final RoutineRepository routines;
  private final RoutineParticipationRepository participations;
  private final RoutineQueryService routineQueries;
  private final ImageRepository images;
  private final ProfileImagePolicy imagePolicy;
  private final ObjectMapper json;

  @Transactional(readOnly = true)
  public Draft findDraft(String routineId, Long userId) {
    return read(inProgress(findRoutine(routineId), userId));
  }

  public Draft saveDraft(String routineId, Long userId, Draft draft) {
    RoutineParticipation participation = inProgress(lockRoutine(routineId), userId);
    validateImages(draft, userId);
    participation.saveWork(json.writeValueAsString(draft));
    return draft;
  }

  public Interactions complete(String routineId, Long userId, Save request) {
    Routine routine = lockRoutine(routineId);
    RoutineParticipation participation = findParticipation(routine, userId);
    if (participation.getStatus() == Status.IN_PROGRESS || participation.getWorkJson() == null) {
      saveResult(participation, userId, request);
      if (participation.getStatus() == Status.IN_PROGRESS) {
        participation.complete(LocalDateTime.now());
      }
    }
    return routineQueries.getInteractions(userId, routineId);
  }

  @Transactional(readOnly = true)
  public Page list(String routineId) {
    List<Summary> results = participations.findPublishedResults(findRoutine(routineId).getId()).stream()
        .map(this::summary)
        .toList();
    return new Page(results, results.size());
  }

  @Transactional(readOnly = true)
  public Detail findResult(String routineId, String participantId) {
    return detail(routineId, published(findRoutine(routineId), participantId));
  }

  public Detail updateResult(String routineId, String participantId, Long userId, Save request) {
    RoutineParticipation participation = ownedResult(lockRoutine(routineId), participantId, userId);
    saveResult(participation, userId, request);
    return detail(routineId, participation);
  }

  public void deleteResult(String routineId, String participantId, Long userId) {
    ownedResult(lockRoutine(routineId), participantId, userId).discard(LocalDateTime.now());
  }

  private Routine findRoutine(String routineId) {
    return routines.findByPublicIdAndDeletedAtIsNull(routineId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));
  }

  private Routine lockRoutine(String routineId) {
    return routines.findActiveForUpdate(routineId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));
  }

  private RoutineParticipation findParticipation(Routine routine, Long userId) {
    if (routine.isOwnedBy(userId)) {
      throw new BusinessException(ErrorStatus.PARTICIPATION_OWNER_FORBIDDEN);
    }
    return participations.findByRoutine_IdAndUser_Id(routine.getId(), userId)
        .filter(p -> p.getDeletedAt() == null)
        .orElseThrow(() -> new BusinessException(ErrorStatus.PARTICIPATION_INVALID_STATE));
  }

  private RoutineParticipation inProgress(Routine routine, Long userId) {
    RoutineParticipation participation = findParticipation(routine, userId);
    if (participation.getStatus() != Status.IN_PROGRESS) {
      throw new BusinessException(ErrorStatus.PARTICIPATION_INVALID_STATE);
    }
    return participation;
  }

  private RoutineParticipation published(Routine routine, String participantId) {
    return participations.findByRoutine_IdAndUser_PublicId(routine.getId(), participantId)
        .filter(p -> p.getDeletedAt() == null && p.getStatus() == Status.COMPLETED && p.getWorkJson() != null)
        .orElseThrow(() -> new BusinessException(ErrorStatus.PARTICIPATION_RESULT_NOT_FOUND));
  }

  private RoutineParticipation ownedResult(Routine routine, String participantId, Long userId) {
    RoutineParticipation participation = published(routine, participantId);
    if (!participation.getUser().getId().equals(userId)) {
      throw new BusinessException(ErrorStatus.PARTICIPATION_RESULT_FORBIDDEN);
    }
    return participation;
  }

  private void saveResult(RoutineParticipation participation, Long userId, Save request) {
    Draft work = Draft.from(request);
    validateImages(work, userId);
    participation.saveWork(json.writeValueAsString(work));
  }

  private void validateImages(Draft work, Long userId) {
    var ids = work.cards().stream().filter(Objects::nonNull).map(DraftCard::imageId)
        .filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      return;
    }
    var keys = images.findUsableKeys(ids, userId);
    if (keys.size() != ids.size()) {
      throw new BusinessException(ErrorStatus.IMAGE_NOT_FOUND);
    }
    keys.forEach(key -> {
      imagePolicy.validateStepSize(key.byteSize());
      imagePolicy.validateStepType(key.contentType());
    });
  }

  private Draft read(RoutineParticipation participation) {
    String work = participation.getWorkJson();
    return work == null ? new Draft(null, null, List.of()) : json.readValue(work, Draft.class);
  }

  private Owner participant(RoutineParticipation participation) {
    var user = participation.getUser();
    return new Owner(user.getPublicId(), user.getNickname(), ImageConverter.toUrl(user.getProfileImage().getPublicId()));
  }

  private Summary summary(RoutineParticipation participation) {
    Draft work = read(participation);
    return new Summary(participant(participation), work.title(), work.content(),
        ImageConverter.toUrl(work.cards().getFirst().imageId()), participation.getCompletedAt());
  }

  private Detail detail(String routineId, RoutineParticipation participation) {
    return new Detail(routineId, participant(participation), read(participation), participation.getCompletedAt());
  }
}
