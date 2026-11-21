package com.example.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserRequestDTO {

  public record Create(
      @Schema(description = "프로필에 표시할 닉네임", example = "야행성")
      @NotBlank @Size(max = 30) String nickname,
      @Schema(description = "마이페이지 한 줄 소개 (선택, 최대 100자)", example = "매일 조금씩 운동해요.", nullable = true)
      @Size(max = 100) @Pattern(regexp = "[^\\r\\n\\x0B\\f\\x85\\u2028\\u2029]*", message = "한 줄 소개에는 줄바꿈을 넣을 수 없습니다.") String bio
  ) {
  }
}
