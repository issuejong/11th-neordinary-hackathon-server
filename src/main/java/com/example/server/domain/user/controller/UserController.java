package com.example.server.domain.user.controller;

import com.example.server.domain.user.dto.UserRequestDTO;
import com.example.server.domain.user.dto.UserResponseDTO;
import com.example.server.domain.user.service.command.UserCommandService;
import com.example.server.domain.user.service.query.UserQueryService;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserCommandService commandService;
  private final UserQueryService queryService;

  @Operation(summary = "내 프로필 조회", description = "현재 점유한 테스트 계정의 프로필을 반환합니다.")
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
