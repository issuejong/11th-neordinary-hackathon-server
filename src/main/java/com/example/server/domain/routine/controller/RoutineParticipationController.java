package com.example.server.domain.routine.controller;

import com.example.server.domain.routine.dto.RoutineResponseDTO.Interactions;
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

@Tag(name = "Routine Participation", description = "루틴 참여 시작 · 취소 · 완료")
@RestController
@RequestMapping("/api/routines/{routineId}/participation")
@RequiredArgsConstructor
public class RoutineParticipationController {

  private final RoutineParticipationCommandService commandService;

  @Operation(summary = "루틴 참여 시작", description = "취소한 참여를 다시 시작하면 시작 시각을 갱신하고 완료 시각을 초기화합니다.")
  @PutMapping
  public ApiResponse<Interactions> join(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.EDITABLE_ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId
  ) {
    return ApiResponse.onSuccess(commandService.join(userId, routineId));
  }

  @Operation(summary = "루틴 참여 취소", description = "진행 중인 참여를 취소합니다. 완료된 참여를 취소하면 409를 반환합니다.")
  @DeleteMapping
  public ApiResponse<Interactions> cancel(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.EDITABLE_ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId
  ) {
    return ApiResponse.onSuccess(commandService.cancel(userId, routineId));
  }

  @Operation(summary = "루틴 참여 완료", description = "진행 중인 참여를 완료 처리합니다. 완료된 참여에 같은 요청을 보내면 완료 상태를 반환합니다.")
  @PutMapping("/completion")
  public ApiResponse<Interactions> complete(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId
  ) {
    return ApiResponse.onSuccess(commandService.complete(userId, routineId));
  }
}
