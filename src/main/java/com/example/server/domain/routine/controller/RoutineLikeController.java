package com.example.server.domain.routine.controller;

import com.example.server.domain.routine.dto.RoutineResponseDTO.Interactions;
import com.example.server.domain.routine.service.command.RoutineLikeCommandService;
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

@Tag(name = "Routine Like", description = "루틴 좋아요 설정 · 취소")
@RestController
@RequestMapping("/api/routines/{routineId}/like")
@RequiredArgsConstructor
public class RoutineLikeController {

  private final RoutineLikeCommandService commandService;

  @Operation(summary = "루틴 좋아요 설정", description = "좋아요를 설정하고 최신 상호작용 집계를 반환합니다.")
  @PutMapping
  public ApiResponse<Interactions> like(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX) String routineId
  ) {
    return ApiResponse.onSuccess(commandService.like(userId, routineId));
  }

  @Operation(summary = "루틴 좋아요 취소", description = "좋아요를 취소하고 최신 상호작용 집계를 반환합니다.")
  @DeleteMapping
  public ApiResponse<Interactions> unlike(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId", example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX) String routineId
  ) {
    return ApiResponse.onSuccess(commandService.unlike(userId, routineId));
  }
}
