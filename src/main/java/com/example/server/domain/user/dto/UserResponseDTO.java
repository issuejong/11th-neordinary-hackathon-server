package com.example.server.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserResponse", description = "사용자 프로필")
public record UserResponseDTO(
    @Schema(description = "사용자 publicId (루틴 목록 authorId 등에 사용)", example = "01KB0000000000000000MEM001")
    String publicId,
    @Schema(description = "닉네임", example = "데모 유저")
    String nickname,
    @Schema(description = "프로필 이미지 publicId (URL: /api/images/{id})", example = "01KB0000000000000000DEF001")
    String profileImagePublicId
) {
}
