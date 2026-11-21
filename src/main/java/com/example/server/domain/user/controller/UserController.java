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
import org.springframework.web.bind.annotation.PatchMapping;
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

  @Operation(summary = "기기의 사용자 목록 조회", description = "첫 화면에서 이 기기에 등록된 프로필을 생성순으로 보여줍니다.")
  @GetMapping
  public ApiResponse<List<UserResponseDTO>> findByDeviceToken(
      @Parameter(description = "기기 토큰", example = DemoData.USER_DEVICE_TOKEN)
      @RequestHeader(LoginUser.HEADER) @NotBlank @Size(max = 255) String deviceToken
  ) {
    return ApiResponse.onSuccess(queryService.findByDeviceToken(deviceToken));
  }

  @Operation(summary = "사용자 생성", description = "같은 기기 토큰으로 프로필을 여러 개 만들 수 있습니다. 응답의 publicId로 프로필을 선택합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<UserResponseDTO>> create(
      @Parameter(description = "기기 토큰", example = "53e93a4d-020e-452c-bdf4-336d106ed693")
      @RequestHeader(LoginUser.HEADER) @NotBlank @Size(max = 255) String deviceToken,
      @Valid @RequestBody UserRequestDTO.Create request
  ) {
    UserResponseDTO result = commandService.create(deviceToken, request);
    return ResponseEntity.created(URI.create("/api/users/me"))
        .body(ApiResponse.of(SuccessStatus._CREATED, result));
  }

  @Operation(summary = "내 프로필 조회", description = "기기에 프로필이 여러 개면 X-User-Id로 선택한 프로필을 반환합니다.")
  @GetMapping("/me")
  public ApiResponse<UserResponseDTO> findMe(@LoginUser Long userId) {
    return ApiResponse.onSuccess(queryService.findMe(userId));
  }

  @Operation(summary = "내 프로필 수정",
      description = "변경할 필드만 전달합니다. 한 줄 소개는 null로 지울 수 있습니다. 대표 이미지는 최대 5MB입니다.")
  @PatchMapping("/me")
  public ApiResponse<UserResponseDTO> updateMe(@LoginUser Long userId,
      @Valid @RequestBody UserRequestDTO.UpdateProfile request) {
    return ApiResponse.onSuccess(commandService.updateProfile(userId, request));
  }
}
