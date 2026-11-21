package com.example.server.domain.routine.service.command;

import com.example.server.domain.image.entity.Image;
import com.example.server.domain.image.repository.ImageKey;
import com.example.server.domain.image.repository.ImageRepository;
import com.example.server.domain.routine.dto.RoutineRequestDTO;
import com.example.server.domain.routine.dto.RoutineResponseDTO;
import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineCard;
import com.example.server.domain.routine.repository.RoutineCardRepository;
import com.example.server.domain.routine.repository.RoutineRepository;
import com.example.server.domain.routine.service.query.RoutineQueryService;
import com.example.server.domain.user.entity.User;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import com.example.server.global.util.Ulid;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoutineCommandServiceImpl implements RoutineCommandService {

  private final RoutineRepository routineRepository;
  private final RoutineCardRepository cardRepository;
  private final ImageRepository imageRepository;
  private final RoutineQueryService routineQueryService;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public RoutineResponseDTO.Detail create(Long userId, RoutineRequestDTO.Save request) {
    User owner = entityManager.getReference(User.class, userId);
    Routine routine = routineRepository.save(
        new Routine(Ulid.generate(), owner, request.title(), request.content()));
    saveCards(routine, request.cards(), userId);
    return routineQueryService.getDetail(routine.getPublicId());
  }

  @Override
  @Transactional
  public RoutineResponseDTO.Detail update(Long userId, String routineId, RoutineRequestDTO.Save request) {
    Routine routine = getOwnedRoutine(userId, routineId);
    routine.update(request.title(), request.content(), userId);
    cardRepository.deleteAllByRoutineId(routine.getId());
    routine = routineRepository.getReferenceById(routine.getId());
    saveCards(routine, request.cards(), userId);
    return routineQueryService.getDetail(routineId);
  }

  @Override
  @Transactional
  public void delete(Long userId, String routineId) {
    getOwnedRoutine(userId, routineId).markDeleted(userId, LocalDateTime.now());
  }

  private Routine getOwnedRoutine(Long userId, String routineId) {
    Routine routine = routineRepository.findByPublicIdAndDeletedAtIsNull(routineId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));
    if (!routine.isOwnedBy(userId)) {
      throw new BusinessException(ErrorStatus.ROUTINE_FORBIDDEN);
    }
    return routine;
  }

  /** 카드 이미지(본인 업로드 또는 기본 이미지)를 한 번에 조회(바이너리 제외)하고, 요청 배열 순서대로 sort_order 1..n 으로 저장한다. */
  private void saveCards(Routine routine, List<RoutineRequestDTO.Card> cards, Long userId) {
    List<String> imageIds = cards.stream().map(RoutineRequestDTO.Card::imageId).distinct().toList();
    Map<String, Long> imageIdMap = imageRepository.findUsableKeys(imageIds, userId).stream()
        .collect(Collectors.toMap(ImageKey::publicId, ImageKey::id));
    if (imageIdMap.size() != imageIds.size()) {
      throw new BusinessException(ErrorStatus.IMAGE_NOT_FOUND);
    }

    List<RoutineCard> entities = new ArrayList<>(cards.size());
    for (int i = 0; i < cards.size(); i++) {
      RoutineRequestDTO.Card card = cards.get(i);
      Image image = imageRepository.getReferenceById(imageIdMap.get(card.imageId()));
      entities.add(new RoutineCard(Ulid.generate(), routine, i + 1, image,
          card.title(), card.content(), card.time()));
    }
    cardRepository.saveAll(entities);
  }
}
