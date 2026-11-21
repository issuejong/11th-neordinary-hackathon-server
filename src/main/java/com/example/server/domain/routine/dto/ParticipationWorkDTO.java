package com.example.server.domain.routine.dto;

import com.example.server.global.util.Ulid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class ParticipationWorkDTO {

  @Schema(name = "ParticipationDraft")
  public record Draft(
      @Schema(example = "함께한 나의 하루") @Size(max = 30) String title,
      @Schema(example = "오늘 같이 해봤어요.") @Size(max = 500) String content,
      @Schema(description = "스텝 순서대로 저장합니다. 빈 스텝 위치에는 null을 넣을 수 있습니다.")
      @Size(max = 10) List<@Valid DraftCard> cards
  ) {
    public Draft {
      if (cards == null) {
        cards = List.of();
      }
    }

    public static Draft from(RoutineRequestDTO.Save request) {
      return new Draft(request.title(), request.content(), request.cards().stream()
          .map(card -> new DraftCard(card.imageId(), card.title(), card.content(), card.time().toString()))
          .toList());
    }
  }

  @Schema(name = "ParticipationDraftCard")
  public record DraftCard(
      @Schema(example = "01KB0000000000000000PHT001") @Pattern(regexp = Ulid.REGEX) String imageId,
      @Schema(example = "기상 후 물 한 잔") @Size(max = 30) String title,
      @Schema(example = "미지근한 물") @Size(max = 100) String content,
      @Schema(example = "07:00") @Pattern(regexp = "(?:[01][0-9]|2[0-3]):[0-5][0-9]") String time
  ) {
  }

  public record Summary(RoutineResponseDTO.Owner participant, String title, String content,
                        String thumbnailUrl, LocalDateTime completedAt) {
  }

  public record Page(List<Summary> results, int totalCount) {
  }

  public record Detail(String routineId, RoutineResponseDTO.Owner participant, Draft work,
                       LocalDateTime completedAt) {
  }
}
