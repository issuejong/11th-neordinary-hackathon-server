package com.example.server.domain.routine.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class RoutineResponseDTO {

  @Schema(description = "루틴 상호작용 집계와 내 상태")
  public record Interactions(
      long likeCount,
      boolean likedByMe,
      long inProgressCount,
      long completedCount,
      @Schema(allowableValues = {"NOT_PARTICIPATING", "IN_PROGRESS", "COMPLETED"}) String myParticipationStatus
  ) {
  }

  @Schema(name = "RoutineDetailResponse", description = "루틴 상세")
  public record Detail(
      @Schema(description = "루틴 publicId", example = "01KB0000000000000000RTN001") String routineId,
      @Schema(description = "루틴 제목", example = "갓생 아침 루틴") String title,
      @Schema(description = "루틴 소개 (없으면 null)", example = "출근 전 1시간으로 하루를 시작해요.") String content,
      @Schema(description = "작성자") Owner owner,
      @Schema(description = "스텝 목록 (order 오름차순)") List<Card> cards,
      @Schema(description = "좋아요 수", example = "0") long likeCount,
      @Schema(description = "댓글 수", example = "0") long commentCount,
      @Schema(description = "KST 기준 생성 시각", example = "2026-11-22T09:30:00.123456") LocalDateTime createdAt,
      @Schema(description = "KST 기준 마지막 수정 시각", example = "2026-11-22T09:30:00.123456") LocalDateTime updatedAt
  ) {
  }

  @Schema(name = "RoutinePageResponse", description = "루틴 목록 (커서 기반 무한 스크롤)")
  public record Page(
      @Schema(description = "루틴 목록 (정렬 순서대로)") List<Summary> routines,
      @Schema(description = "필터(authorId · participantId)에 해당하는 전체 루틴 수 — 프로필 탭 개수 표시용", example = "3") long totalCount,
      @Schema(description = "요청한 페이지 크기", example = "20") int size,
      @Schema(description = "다음 페이지 존재 여부", example = "true") boolean hasNext,
      @Schema(description = "다음 페이지 요청 시 cursor로 보낼 값 (마지막 페이지면 null)", example = "TEFURVNUfDB8MDFNN1o4N1dBMVdSS0QxNk43TTI2NkVKQjI")
      String nextCursor
  ) {
  }

  @Schema(name = "RoutineSummaryResponse", description = "루틴 목록 항목")
  public record Summary(
      @Schema(description = "루틴 publicId", example = "01KB0000000000000000RTN001") String routineId,
      @Schema(description = "루틴 제목", example = "갓생 아침 루틴") String title,
      @Schema(description = "루틴 소개 (없으면 null)", example = "출근 전 1시간으로 하루를 시작해요.") String content,
      @Schema(description = "작성자") Owner owner,
      @Schema(description = "썸네일 = 첫 번째 스텝 이미지 URL", example = "https://neo8.duckdns.org/api/images/01KB0000000000000000PHT001") String thumbnailUrl,
      @Schema(description = "스텝 개수", example = "3") long cardCount,
      @Schema(description = "좋아요 수", example = "0") long likeCount,
      @Schema(description = "참여자 수 (참여 중 + 완료, 취소 제외)", example = "0") long participantCount,
      boolean likedByMe,
      long inProgressCount,
      long completedCount,
      String myParticipationStatus,
      @Schema(description = "댓글 수 (삭제 제외)", example = "0") long commentCount,
      @Schema(description = "KST 기준 생성 시각", example = "2026-11-22T09:30:00.123456") LocalDateTime createdAt
  ) {
  }

  @Schema(name = "RoutineOwnerResponse", description = "루틴 작성자")
  public record Owner(
      @Schema(description = "사용자 publicId", example = "01KB0000000000000000MEM001") String userId,
      @Schema(description = "닉네임", example = "데모 유저") String nickname,
      @Schema(description = "프로필 이미지 URL (바로 표시)", example = "https://neo8.duckdns.org/api/images/01KB0000000000000000DEF001") String profileImageUrl
  ) {
  }

  @Schema(name = "RoutineCardResponse", description = "루틴 카드")
  public record Card(
      @Schema(description = "스텝 publicId", example = "01M7Z87V9Y4B5RV3ADW719R5BW") String cardId,
      @Schema(description = "스텝 순서 (1부터 연속)", example = "1") int order,
      @Schema(description = "스텝 제목", example = "기상 후 물 한 잔 마시기") String title,
      @Schema(description = "스텝 내용 (없으면 null)", example = "미지근한 물 500ml") String content,
      @Schema(description = "스텝 시간 HH:mm", type = "string", example = "07:00") @JsonFormat(pattern = "HH:mm") LocalTime time,
      @Schema(description = "스텝 이미지 publicId (수정 요청에 그대로 재사용)", example = "01KB0000000000000000PHT001") String imageId,
      @Schema(description = "스텝 이미지 URL (바로 표시)", example = "https://neo8.duckdns.org/api/images/01KB0000000000000000PHT001") String imageUrl
  ) {
  }
}
