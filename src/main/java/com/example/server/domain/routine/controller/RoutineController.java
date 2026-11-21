package com.example.server.domain.routine.controller;

import com.example.server.domain.routine.dto.RoutineRequestDTO;
import com.example.server.domain.routine.dto.RoutineResponseDTO;
import com.example.server.domain.routine.service.command.RoutineCommandService;
import com.example.server.domain.routine.service.query.RoutineQueryService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Routine", description = "루틴 생성 · 상세 조회 · 수정 · 삭제")
@RestController
@RequestMapping("/api/routines")
@RequiredArgsConstructor
public class RoutineController {

  private static final String ID_MESSAGE = "올바른 ID 형식(ULID 26자)이 아닙니다.";

  private final RoutineCommandService commandService;
  private final RoutineQueryService queryService;

  @Operation(summary = "루틴 생성", description = "카드 1~10장을 함께 등록합니다. 카드 이미지는 이미지 업로드 API로 먼저 올린 imageId를 사용합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<RoutineResponseDTO.Detail>> create(
      @LoginUser Long userId,
      @Valid @RequestBody RoutineRequestDTO.Save request
  ) {
    RoutineResponseDTO.Detail result = commandService.create(userId, request);
    return ResponseEntity.created(URI.create("/api/routines/" + result.routineId()))
        .body(ApiResponse.of(SuccessStatus._CREATED, result));
  }

  @Operation(summary = "루틴 상세 조회", description = "루틴 기본 정보, 작성자, 카드 목록(순서대로), 좋아요·댓글 수를 반환합니다.")
  @GetMapping("/{routineId}")
  public ApiResponse<RoutineResponseDTO.Detail> find(
      @Parameter(description = "루틴 publicId", example = DemoData.ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = ID_MESSAGE) String routineId
  ) {
    return ApiResponse.onSuccess(queryService.getDetail(routineId));
  }

  @Operation(summary = "루틴 수정", description = "제목·소개를 수정하고 카드 목록은 요청 내용으로 전체 교체합니다. 작성자만 가능합니다.")
  @PutMapping("/{routineId}")
  public ApiResponse<RoutineResponseDTO.Detail> update(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId (데모 사용자의 수정·삭제용 루틴)", example = DemoData.EDITABLE_ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = ID_MESSAGE) String routineId,
      @Valid @RequestBody RoutineRequestDTO.Save request
  ) {
    return ApiResponse.onSuccess(commandService.update(userId, routineId, request));
  }

  @Operation(summary = "루틴 삭제", description = "소프트 삭제합니다. 삭제 후에는 조회·수정 시 404를 반환합니다. 작성자만 가능합니다.")
  @DeleteMapping("/{routineId}")
  public ApiResponse<Void> delete(
      @LoginUser Long userId,
      @Parameter(description = "루틴 publicId (데모 사용자의 수정·삭제용 루틴)", example = DemoData.EDITABLE_ROUTINE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = ID_MESSAGE) String routineId
  ) {
    commandService.delete(userId, routineId);
    return ApiResponse.onSuccess(null);
  }
}
