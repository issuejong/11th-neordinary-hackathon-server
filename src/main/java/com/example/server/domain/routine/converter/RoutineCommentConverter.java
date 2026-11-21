package com.example.server.domain.routine.converter;

import com.example.server.domain.image.converter.ImageConverter;
import com.example.server.domain.routine.dto.RoutineCommentResponseDTO;
import com.example.server.domain.routine.repository.projection.RoutineCommentView;

public final class RoutineCommentConverter {

  private RoutineCommentConverter() {
  }

  public static RoutineCommentResponseDTO toResponse(RoutineCommentView comment, Long viewerId) {
    return new RoutineCommentResponseDTO(
        comment.publicId(), comment.authorNickname(), ImageConverter.toUrl(comment.profileImagePublicId()),
        comment.content(), comment.createdAt(), comment.authorId().equals(viewerId));
  }
}
