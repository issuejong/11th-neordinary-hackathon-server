package com.example.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TestAccountRequestDTO {

  public static final int MAX_SLOTS = 7;

  @Schema(name = "TestAccountRegisterRequest")
  public record Register(
      @Schema(description = "첫 화면에 표시할 계정 위치 (1~" + MAX_SLOTS + ")", example = "1")
      @NotNull @Min(1) @Max(MAX_SLOTS) Integer slot,
      @Schema(description = "테스트 계정 닉네임 (2~10자)", example = "아침친구")
      @NotBlank @Size(max = 30) String nickname,
      @Schema(description = "선택 입력인 한 줄 소개", example = "매일 산책해요.", nullable = true)
      @Size(max = 50) @Pattern(regexp = "[^\\r\\n\\x0B\\f\\x85\\u2028\\u2029]*") String bio
  ) {
  }
}
