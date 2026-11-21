package com.example.server.domain.routine.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class RoutineResponseDTO {

  @Schema(description = "루틴 상세")
  public record Detail(
      @Schema(description = "루틴 publicId", example = "01KB0000000000000000RTN001") String routineId,
      @Schema(example = "갓생 아침 루틴") String title,
      @Schema(example = "출근 전 1시간으로 하루를 시작해요.") String content,
      Owner owner,
      List<Card> cards,
      @Schema(description = "좋아요 수", example = "0") long likeCount,
      @Schema(description = "댓글 수", example = "0") long commentCount,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {
  }

  @Schema(description = "루틴 목록 (커서 기반 무한 스크롤)")
  public record Page(
      List<Summary> routines,
      @Schema(description = "요청한 페이지 크기", example = "20") int size,
      @Schema(description = "다음 페이지 존재 여부", example = "true") boolean hasNext,
      @Schema(description = "다음 페이지 요청 시 cursor로 보낼 값 (마지막 페이지면 null)", example = "TEFURVNUfDB8MDFNN1o4N1dBMVdSS0QxNk43TTI2NkVKQjI")
      String nextCursor
  ) {
  }

  @Schema(description = "루틴 목록 항목")
  public record Summary(
      @Schema(description = "루틴 publicId", example = "01KB0000000000000000RTN001") String routineId,
      @Schema(example = "갓생 아침 루틴") String title,
      @Schema(example = "출근 전 1시간으로 하루를 시작해요.") String content,
      Owner owner,
      @Schema(description = "썸네일 (첫 번째 스텝 이미지 URL)") String thumbnailUrl,
      @Schema(description = "스텝 개수", example = "3") long cardCount,
      @Schema(description = "좋아요 수", example = "0") long likeCount,
      @Schema(description = "참여자 수 (참여 중 + 완료, 취소 제외)", example = "0") long participantCount,
      LocalDateTime createdAt
  ) {
  }

  @Schema(description = "루틴 작성자")
  public record Owner(
      @Schema(description = "사용자 publicId", example = "01KB0000000000000000MEM001") String userId,
      @Schema(example = "데모 유저") String nickname,
      @Schema(description = "프로필 이미지 URL") String profileImageUrl
  ) {
  }

  @Schema(description = "루틴 카드")
  public record Card(
      @Schema(description = "카드 publicId") String cardId,
      @Schema(description = "카드 순서 (1부터)", example = "1") int order,
      @Schema(example = "기상 후 물 한 잔 마시기") String title,
      @Schema(example = "미지근한 물 500ml") String content,
      @Schema(type = "string", example = "07:00") @JsonFormat(pattern = "HH:mm") LocalTime time,
      @Schema(description = "이미지 publicId") String imageId,
      @Schema(description = "이미지 URL") String imageUrl
  ) {
  }
}
