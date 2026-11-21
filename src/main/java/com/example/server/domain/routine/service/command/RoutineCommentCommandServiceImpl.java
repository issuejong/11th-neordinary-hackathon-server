package com.example.server.domain.routine.service.command;

import com.example.server.domain.routine.converter.RoutineCommentConverter;
import com.example.server.domain.routine.dto.RoutineCommentRequestDTO.CommentBody;
import com.example.server.domain.routine.dto.RoutineCommentResponseDTO;
import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineComment;
import com.example.server.domain.routine.repository.RoutineCommentRepository;
import com.example.server.domain.routine.repository.RoutineRepository;
import com.example.server.domain.user.entity.User;
import com.example.server.domain.user.repository.UserRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import com.example.server.global.util.Ulid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RoutineCommentCommandServiceImpl implements RoutineCommentCommandService {

  private final RoutineRepository routineRepository;
  private final RoutineCommentRepository commentRepository;
  private final UserRepository userRepository;

  @Override
  public RoutineCommentResponseDTO create(Long userId, String routineId, CommentBody request) {
    Routine routine = getRoutine(routineId);
    User author = userRepository.getReferenceById(userId);
    RoutineComment comment = commentRepository.save(new RoutineComment(Ulid.generate(), routine, author, request.content()));
    return getResponse(comment.getPublicId(), userId);
  }

  @Override
  public RoutineCommentResponseDTO update(Long userId, String routineId, String commentId, CommentBody request) {
    RoutineComment comment = getOwnedComment(userId, routineId, commentId);
    comment.edit(request.content());
    return getResponse(commentId, userId);
  }

  @Override
  public void delete(Long userId, String routineId, String commentId) {
    getOwnedComment(userId, routineId, commentId).markDeleted(userId, LocalDateTime.now());
  }

  private Routine getRoutine(String routineId) {
    return routineRepository.findByPublicIdAndDeletedAtIsNull(routineId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.ROUTINE_NOT_FOUND));
  }

  private RoutineComment getOwnedComment(Long userId, String routineId, String commentId) {
    Routine routine = getRoutine(routineId);
    RoutineComment comment = commentRepository.findByRoutine_IdAndPublicIdAndDeletedAtIsNull(routine.getId(), commentId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.COMMENT_NOT_FOUND));
    if (!comment.getUser().getId().equals(userId)) {
      throw new BusinessException(ErrorStatus.COMMENT_FORBIDDEN);
    }
    return comment;
  }

  private RoutineCommentResponseDTO getResponse(String commentId, Long userId) {
    return commentRepository.findActiveView(commentId)
        .map(comment -> RoutineCommentConverter.toResponse(comment, userId))
        .orElseThrow(() -> new BusinessException(ErrorStatus.COMMENT_NOT_FOUND));
  }
}
