package com.example.server.domain.routine.service.query;

import com.example.server.domain.routine.converter.RoutineConverter;
import com.example.server.domain.routine.dto.RoutineResponseDTO;
import com.example.server.domain.routine.dto.RoutineSort;
import com.example.server.domain.routine.entity.RoutineRankingSnapshot;
import com.example.server.domain.routine.repository.RoutineRankingSnapshotRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import org.springframework.scheduling.annotation.Scheduled;
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

  private static final Duration RANKING_CURSOR_TTL = Duration.ofMinutes(15);

  private final RoutineRepository routineRepository;
  private final RoutineRankingSnapshotRepository rankings;
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
  @Transactional
  public RoutineResponseDTO.Page getList(RoutineSort sort, String authorId, String participantId, String cursor,
      int size, Long viewerId) {
    RoutineCursor from = RoutineCursor.decode(cursor, sort);
    if (sort != RoutineSort.LATEST) {
      return rankedPage(sort, authorId, participantId, cursor, from, size, viewerId);
    }

    Pageable limit = PageRequest.of(0, size + 1);
    List<RoutineSummaryView> rows = routineRepository.findLatest(authorId, participantId, from.publicId(), limit);
    boolean hasNext = rows.size() > size;
    List<RoutineSummaryView> page = hasNext ? rows.subList(0, size) : rows;
    String nextCursor = hasNext ? RoutineCursor.after(sort, 0, page.getLast().publicId()).encode() : null;
    return toPage(page, routineRepository.countForList(authorId, participantId), size, nextCursor, viewerId);
  }

  private RoutineResponseDTO.Page rankedPage(RoutineSort sort, String authorId, String participantId,
      String cursor, RoutineCursor from, int size, Long viewerId) {
    Instant now = Instant.now();
    boolean first = cursor == null || cursor.isBlank();
    RoutineRankingSnapshot snapshot = first ? null : rankings.findById(from.publicId())
        .filter(ranking -> ranking.matches(sort, authorId, participantId, now))
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_INVALID_CURSOR));
    List<String> ids = first
        ? (sort == RoutineSort.LIKES ? routineRepository.findIdsByLikes(authorId, participantId)
            : routineRepository.findIdsByParticipants(authorId, participantId))
        : snapshot.ids();
    if (!first && (from.count() == 0 || from.count() >= ids.size())) {
      throw new BusinessException(ErrorStatus.ROUTINE_INVALID_CURSOR);
    }

    List<RoutineSummaryView> page = new ArrayList<>(size);
    int offset = first ? 0 : Math.toIntExact(from.count());
    int next = appendRankedRows(ids, offset, size, authorId, participantId, page);
    String nextCursor = null;
    if (next >= 0) {
      if (first) {
        snapshot = rankings.save(new RoutineRankingSnapshot(sort, authorId, participantId, ids,
            now.plus(RANKING_CURSOR_TTL)));
      }
      nextCursor = RoutineCursor.after(sort, next, snapshot.getPublicId()).encode();
    }
    return toPage(page, ids.size(), size, nextCursor, viewerId);
  }

  private int appendRankedRows(List<String> ids, int offset, int size, String authorId, String participantId,
      List<RoutineSummaryView> page) {
    while (offset < ids.size()) {
      List<String> batch = ids.subList(offset, Math.min(ids.size(), offset + size + 1));
      var byId = routineRepository.findRankedPage(batch, authorId, participantId).stream()
          .collect(Collectors.toMap(RoutineSummaryView::publicId, Function.identity()));
      for (String id : batch) {
        RoutineSummaryView row = byId.get(id);
        if (row != null) {
          if (page.size() == size) {
            return offset;
          }
          page.add(row);
        }
        offset++;
      }
    }
    return -1;
  }

  private RoutineResponseDTO.Page toPage(List<RoutineSummaryView> page, long totalCount, int size,
      String nextCursor, Long viewerId) {
    Map<String, RoutineListInteractionView> interactions = page.isEmpty() ? Map.of()
        : routineRepository.findListInteractions(page.stream().map(RoutineSummaryView::publicId).toList(), viewerId)
            .stream().collect(Collectors.toMap(RoutineListInteractionView::routineId, Function.identity()));
    return RoutineConverter.toPage(page, interactions, totalCount, size, nextCursor);
  }

  @Scheduled(fixedDelayString = "${app.routine.ranking-cleanup-interval:5m}")
  @Transactional
  public void deleteExpiredRankings() {
    rankings.deleteExpired(Instant.now());
  }
}
