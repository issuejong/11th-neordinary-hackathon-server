package com.example.server.domain.routine.controller;

import com.example.server.domain.routine.dto.ParticipationWorkDTO.Detail;
import com.example.server.domain.routine.dto.ParticipationWorkDTO.Page;
import com.example.server.domain.routine.dto.RoutineRequestDTO.Save;
import com.example.server.domain.routine.service.ParticipationWorkService;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.response.ApiResponse;
import com.example.server.global.util.Ulid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.example.server.global.init.DemoData;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Routine Participation Results")
@RestController
@RequestMapping("/api/routines/{routineId}/participation-results")
@RequiredArgsConstructor
public class ParticipationResultController {

  private final ParticipationWorkService works;

  @Operation(summary = "함께한 루틴 목록", description = "공개된 참여 결과를 완료 최신순으로 반환합니다.")
  @GetMapping
  public ApiResponse<Page> list(
      @Parameter(example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId) {
    return ApiResponse.onSuccess(works.list(routineId));
  }

  @Operation(summary = "함께한 루틴 상세")
  @GetMapping("/{participantId}")
  public ApiResponse<Detail> detail(
      @Parameter(example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId,
      @Parameter(description = "함께한 루틴 목록 응답의 participant.userId")
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String participantId) {
    return ApiResponse.onSuccess(works.findResult(routineId, participantId));
  }

  @Operation(summary = "내 참여 결과 수정")
  @PutMapping("/{participantId}")
  public ApiResponse<Detail> update(@LoginUser Long userId,
      @Parameter(example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId,
      @Parameter(description = "함께한 루틴 목록 응답의 participant.userId")
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String participantId,
      @Valid @RequestBody Save request) {
    return ApiResponse.onSuccess(works.updateResult(routineId, participantId, userId, request));
  }

  @Operation(summary = "내 참여 결과 삭제")
  @DeleteMapping("/{participantId}")
  public ApiResponse<Void> delete(@LoginUser Long userId,
      @Parameter(example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String routineId,
      @Parameter(description = "함께한 루틴 목록 응답의 participant.userId")
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String participantId) {
    works.deleteResult(routineId, participantId, userId);
    return ApiResponse.onSuccess(null);
  }
}
