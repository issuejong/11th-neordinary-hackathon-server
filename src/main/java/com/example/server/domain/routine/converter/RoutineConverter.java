package com.example.server.domain.routine.converter;

import com.example.server.domain.image.converter.ImageConverter;
import com.example.server.domain.routine.dto.RoutineResponseDTO;
import com.example.server.domain.routine.repository.projection.RoutineCardView;
import com.example.server.domain.routine.repository.projection.RoutineHeaderView;
import java.util.List;

public final class RoutineConverter {

  private RoutineConverter() {
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
