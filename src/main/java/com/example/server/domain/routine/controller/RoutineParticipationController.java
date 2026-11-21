package com.example.server.domain.routine.controller;

import com.example.server.domain.routine.dto.RoutineResponseDTO.Interactions;
import com.example.server.domain.routine.dto.RoutineRequestDTO.Save;
import com.example.server.domain.routine.dto.ParticipationWorkDTO.Draft;
import com.example.server.domain.routine.service.ParticipationWorkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.server.domain.routine.service.command.RoutineParticipationCommandService;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.global.response.ApiResponse;
import com.example.server.global.util.Ulid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Routine Participation")
@RestController
@RequestMapping("/api/routines/{routineId}/participation")
@RequiredArgsConstructor
public class RoutineParticipationController {

  private final RoutineParticipationCommandService commandService;
  private final ParticipationWorkService works;

  @Operation(summary = "루틴 참여 시작", description = "취소한 참여를 다시 시작하면 시작 시각을 갱신하고 완료 시각을 초기화합니다.")
  @PutMapping
  public ApiResponse<Interactions> join(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.JOIN_ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId
  ) {
    return ApiResponse.onSuccess(commandService.join(userId, routineId));
  }

  @Operation(summary = "루틴 참여 취소", description = "진행 중인 참여를 취소합니다. 완료된 참여를 취소하면 409를 반환합니다.")
  @DeleteMapping
  public ApiResponse<Interactions> cancel(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.JOIN_ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId
  ) {
    return ApiResponse.onSuccess(commandService.cancel(userId, routineId));
  }

  @Operation(summary = "내 루틴 참여 초안 조회")
  @GetMapping("/draft")
  public ApiResponse<Draft> draft(@LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.DRAFT_ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId) {
    return ApiResponse.onSuccess(works.findDraft(routineId, userId));
  }

  @Operation(summary = "내 루틴 참여 초안 저장", description = "현재 작성 내용을 전체 교체합니다.")
  @PutMapping("/draft")
  public ApiResponse<Draft> saveDraft(@LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.DRAFT_ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId,
      @Valid @RequestBody Draft request) {
    return ApiResponse.onSuccess(works.saveDraft(routineId, userId, request));
  }

  @Operation(summary = "루틴 참여 완료", description = "작성한 제목·소개·스텝을 보내 완료 결과를 공개합니다.")
  @PutMapping("/completion")
  public ApiResponse<Interactions> complete(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.COMPLETE_ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId,
      @Valid @RequestBody(required = false) Save request
  ) {
    return ApiResponse.onSuccess(request == null ? commandService.complete(userId, routineId)
        : works.complete(routineId, userId, request));
  }
}
