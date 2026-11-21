package com.example.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequestDTO {

  public record Create(
      @Schema(description = "프로필에 표시할 닉네임", example = "야행성")
      @NotBlank @Size(max = 30) String nickname
  ) {
  }
}
