package com.example.server.domain.routine.service.query;

import com.example.server.domain.routine.converter.RoutineConverter;
import com.example.server.domain.routine.dto.RoutineResponseDTO;
import com.example.server.domain.routine.dto.RoutineSort;
import com.example.server.domain.routine.repository.RoutineCardRepository;
import com.example.server.domain.routine.repository.RoutineCommentRepository;
import com.example.server.domain.routine.repository.RoutineLikeRepository;
import com.example.server.domain.routine.repository.RoutineRepository;
import com.example.server.domain.routine.repository.projection.RoutineHeaderView;
import com.example.server.domain.routine.repository.projection.RoutineSummaryView;
import com.example.server.domain.routine.support.RoutineCursor;
import com.example.server.domain.routine.repository.projection.RoutineListInteractionView;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

  @Override
  @Transactional(readOnly = true)
  public RoutineResponseDTO.Interactions getInteractions(Long userId, String routineId) {
    return routineRepository.findInteractions(routineId, userId)
        .map(RoutineConverter::toInteractions)
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));
  }

  @Override
  @Transactional(readOnly = true)
  public RoutineResponseDTO.Page getList(RoutineSort sort, String authorId, String participantId, String cursor,
      int size, Long viewerId) {
    RoutineCursor from = RoutineCursor.decode(cursor, sort);
    Pageable limit = PageRequest.of(0, size + 1); // 한 개 더 조회해서 다음 페이지 여부 판단
    List<RoutineSummaryView> rows = switch (sort) {
      case LATEST -> routineRepository.findLatest(authorId, participantId, from.publicId(), limit);
      case LIKES -> routineRepository.findMostLiked(authorId, participantId, from.count(), from.publicId(), limit);
      case PARTICIPANTS -> routineRepository.findMostParticipated(authorId, participantId, from.count(), from.publicId(), limit);
    };

    boolean hasNext = rows.size() > size;
    List<RoutineSummaryView> page = hasNext ? rows.subList(0, size) : rows;
    String nextCursor = hasNext ? nextCursor(sort, page.getLast()) : null;
    Map<String, RoutineListInteractionView> interactions = page.isEmpty() ? Map.of()
        : routineRepository.findListInteractions(page.stream().map(RoutineSummaryView::publicId).toList(), viewerId)
            .stream().collect(Collectors.toMap(RoutineListInteractionView::routineId, Function.identity()));
    return RoutineConverter.toPage(page, interactions, routineRepository.countForList(authorId, participantId), size, nextCursor);
  }

  private static String nextCursor(RoutineSort sort, RoutineSummaryView last) {
    long count = switch (sort) {
      case LATEST -> 0;
      case LIKES -> last.likeCount();
      case PARTICIPANTS -> last.participantCount();
    };
    return RoutineCursor.after(sort, count, last.publicId()).encode();
  }
}
