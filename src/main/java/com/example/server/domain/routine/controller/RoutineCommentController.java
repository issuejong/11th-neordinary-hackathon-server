package com.example.server.domain.routine.controller;

import com.example.server.domain.routine.dto.RoutineCommentRequestDTO.CommentBody;
import com.example.server.domain.routine.dto.RoutineCommentResponseDTO;
import com.example.server.domain.routine.service.command.RoutineCommentCommandService;
import com.example.server.domain.routine.service.query.RoutineCommentQueryService;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.global.response.ApiResponse;
import com.example.server.global.response.code.status.SuccessStatus;
import com.example.server.global.util.Ulid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Routine Comment")
@RestController
@RequestMapping("/api/routines/{routineId}/comments")
@RequiredArgsConstructor
public class RoutineCommentController {

  private final RoutineCommentCommandService commandService;
  private final RoutineCommentQueryService queryService;

  @Operation(summary = "루틴 댓글 목록 조회",
      description = "작성순으로 조회합니다. 점유 세션을 보내면 내 댓글에 isMine을 true로 반환합니다. 생략하면 isMine은 모두 false입니다.")
  @GetMapping
  public ApiResponse<List<RoutineCommentResponseDTO>> findAll(
      @LoginUser(required = false) Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId
  ) {
    return ApiResponse.onSuccess(queryService.findAll(userId, routineId));
  }

  @Operation(summary = "루틴 댓글 작성", description = "본문 1~50자. 생성된 댓글을 반환합니다(isMine=true).")
  @PostMapping
  public ResponseEntity<ApiResponse<RoutineCommentResponseDTO>> create(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId,
      @Valid @RequestBody CommentBody request
  ) {
    RoutineCommentResponseDTO result = commandService.create(userId, routineId, request);
    return ResponseEntity.created(URI.create("/api/routines/" + routineId + "/comments/" + result.commentId()))
        .body(ApiResponse.of(SuccessStatus._CREATED, result));
  }

  @Operation(summary = "루틴 댓글 수정", description = "댓글 작성자만 본문을 수정할 수 있습니다.")
  @PatchMapping("/{commentId}")
  public ApiResponse<RoutineCommentResponseDTO> update(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId,
      @Parameter(description = "댓글 publicId", example = DemoData.COMMENT_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String commentId,
      @Valid @RequestBody CommentBody request
  ) {
    return ApiResponse.onSuccess(commandService.update(userId, routineId, commentId, request));
  }

  @Operation(summary = "루틴 댓글 삭제", description = "작성자만 삭제할 수 있습니다. 예시 댓글이 이미 삭제됐다면 새로 작성한 댓글 ID를 입력하세요.")
  @DeleteMapping("/{commentId}")
  public ApiResponse<Void> delete(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId,
      @Parameter(description = "댓글 publicId", example = DemoData.DELETABLE_COMMENT_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String commentId
  ) {
    commandService.delete(userId, routineId, commentId);
    return ApiResponse.onSuccess(null);
  }
}
