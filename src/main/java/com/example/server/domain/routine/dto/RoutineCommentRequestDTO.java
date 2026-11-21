package com.example.server.domain.routine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RoutineCommentRequestDTO {

  public record CommentBody(
      @Schema(description = "댓글 본문 (1~50자)", example = "오늘부터 해볼게요.")
      @NotBlank @Size(max = 50) String content
  ) {
  }
}
