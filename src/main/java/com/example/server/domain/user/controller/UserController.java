package com.example.server.domain.user.controller;

import com.example.server.domain.user.dto.UserRequestDTO;
import com.example.server.domain.user.dto.UserResponseDTO;
import com.example.server.domain.user.service.command.UserCommandService;
import com.example.server.domain.user.service.query.UserQueryService;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.global.response.ApiResponse;
import com.example.server.global.response.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserCommandService commandService;
  private final UserQueryService queryService;

  @Operation(summary = "기기 토큰으로 사용자 조회", description = "앱 시작 시 이 기기로 가입한 사용자가 있는지 확인합니다. 없으면 빈 배열이고, 이때 `POST /api/users`로 가입합니다.")
  @GetMapping
  public ApiResponse<List<UserResponseDTO>> findByDeviceToken(
      @Parameter(description = "기기 토큰", example = DemoData.USER_DEVICE_TOKEN)
      @RequestHeader(LoginUser.HEADER) @NotBlank @Size(max = 255) String deviceToken
  ) {
    return ApiResponse.onSuccess(queryService.findByDeviceToken(deviceToken));
  }

  @Operation(summary = "사용자 생성", description = "미등록 기기 토큰으로 사용자를 생성합니다. 이미 등록된 토큰은 409를 반환합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<UserResponseDTO>> create(
      @Parameter(description = "아직 등록하지 않은 기기 토큰", example = "53e93a4d-020e-452c-bdf4-336d106ed693")
      @RequestHeader(LoginUser.HEADER) @NotBlank @Size(max = 255) String deviceToken,
      @Valid @RequestBody UserRequestDTO.Create request
  ) {
    UserResponseDTO result = commandService.create(deviceToken, request);
    return ResponseEntity.created(URI.create("/api/users/me"))
        .body(ApiResponse.of(SuccessStatus._CREATED, result));
  }

  @Operation(summary = "내 프로필 조회", description = "X-Device-Token 헤더의 사용자 프로필을 반환합니다.")
  @GetMapping("/me")
  public ApiResponse<UserResponseDTO> findMe(@LoginUser Long userId) {
    return ApiResponse.onSuccess(queryService.findMe(userId));
  }
}
