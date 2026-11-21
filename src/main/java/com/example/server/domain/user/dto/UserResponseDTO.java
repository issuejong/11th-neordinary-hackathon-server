package com.example.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponseDTO(String publicId, String nickname, String profileImagePublicId,
                              @Schema(description = "마이페이지 한 줄 소개", nullable = true) String bio) {
}
