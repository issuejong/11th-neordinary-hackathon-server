package com.example.server.domain.routine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(name = "RoutineCommentResponse", description = "루틴 댓글")
public record RoutineCommentResponseDTO(
    @Schema(description = "댓글 publicId (수정 · 삭제에 사용)", example = "01KB0000000000000000CMT001")
    String commentId,
    @Schema(description = "작성자 닉네임", example = "데모 유저")
    String authorNickname,
    @Schema(description = "작성자 프로필 이미지 URL (바로 표시)", example = "https://neo8.duckdns.org/api/images/01KB0000000000000000DEF001")
    String profileImageUrl,
    @Schema(description = "댓글 내용 (1~50자)", example = "오늘부터 해볼게요.")
    String content,
    @Schema(description = "KST 기준 작성 시각", example = "2026-11-22T09:30:00.123456")
    LocalDateTime createdAt,
    @Schema(description = "내가 쓴 댓글이면 true (수정 · 삭제 버튼 표시용)", example = "true")
    boolean isMine
) {
}
