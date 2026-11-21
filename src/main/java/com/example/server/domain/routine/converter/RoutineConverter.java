package com.example.server.domain.routine.converter;

import com.example.server.domain.image.converter.ImageConverter;
import com.example.server.domain.routine.dto.RoutineResponseDTO;
import com.example.server.domain.routine.repository.projection.RoutineCardView;
import com.example.server.domain.routine.repository.projection.RoutineHeaderView;
import com.example.server.domain.routine.repository.projection.RoutineInteractionView;
import com.example.server.domain.routine.repository.projection.RoutineSummaryView;
import com.example.server.domain.routine.repository.projection.RoutineListInteractionView;
import java.util.List;
import java.util.Map;

public final class RoutineConverter {

  private RoutineConverter() {
  }

  public static RoutineResponseDTO.Interactions toInteractions(RoutineInteractionView view) {
    String status = view.myParticipationStatus() == null ? "NOT_PARTICIPATING" : view.myParticipationStatus().name();
    return new RoutineResponseDTO.Interactions(view.likeCount(), view.myLikeCount() > 0, view.inProgressCount(),
        view.completedCount(), status);
  }

  public static RoutineResponseDTO.Detail toDetail(RoutineHeaderView header, List<RoutineCardView> cards,
      long likeCount, long commentCount) {
    return new RoutineResponseDTO.Detail(
        header.publicId(),
        header.title(),
        header.content(),
        new RoutineResponseDTO.Owner(
            header.ownerPublicId(),
            header.ownerNickname(),
            ImageConverter.toUrl(header.ownerProfileImagePublicId())),
        cards.stream().map(RoutineConverter::toCard).toList(),
        likeCount,
        commentCount,
        header.createdAt(),
        header.updatedAt());
  }

  public static RoutineResponseDTO.Page toPage(List<RoutineSummaryView> views, Map<String, RoutineListInteractionView> interactions,
      long totalCount, int size,
      String nextCursor) {
    return new RoutineResponseDTO.Page(
        views.stream().map(view -> toSummary(view, interactions.get(view.publicId()))).toList(), totalCount, size, nextCursor != null, nextCursor);
  }

  private static RoutineResponseDTO.Summary toSummary(RoutineSummaryView view, RoutineListInteractionView interactions) {
    return new RoutineResponseDTO.Summary(
        view.publicId(),
        view.title(),
        view.content(),
        new RoutineResponseDTO.Owner(
            view.ownerPublicId(),
            view.ownerNickname(),
            ImageConverter.toUrl(view.ownerProfileImagePublicId())),
        view.thumbnailImagePublicId() == null ? null : ImageConverter.toUrl(view.thumbnailImagePublicId()),
        view.cardCount(),
        view.likeCount(),
        view.participantCount(),
        interactions.myLikeCount() > 0,
        interactions.inProgressCount(),
        interactions.completedCount(),
        interactions.myStatus() == null ? "NOT_PARTICIPATING" : interactions.myStatus().name(),
        view.commentCount(),
        view.createdAt());
  }

  private static RoutineResponseDTO.Card toCard(RoutineCardView card) {
    return new RoutineResponseDTO.Card(
        card.publicId(),
        card.sortOrder(),
        card.title(),
        card.content(),
        card.cardTime(),
        card.imagePublicId(),
        ImageConverter.toUrl(card.imagePublicId()));
  }
}
