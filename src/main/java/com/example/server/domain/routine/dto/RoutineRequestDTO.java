package com.example.server.domain.routine.dto;

import com.example.server.global.init.DemoData;
import com.example.server.global.util.Ulid;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

public class RoutineRequestDTO {

  /** 루틴 생성 / 전체 수정 공용 요청 (수정 시 카드 목록은 통째로 교체) */
  @Schema(description = "루틴 생성 · 수정 요청")
  public record Save(
      @Schema(description = "루틴 제목 (1~30자)", example = "갓생 아침 루틴", requiredMode = Schema.RequiredMode.REQUIRED)
      @NotBlank(message = "루틴 제목은 필수입니다.")
      @Size(max = 30, message = "루틴 제목은 30자 이하여야 합니다.")
      String title,

      @Schema(description = "루틴 소개 (선택, 최대 100자)", example = "출근 전 1시간으로 하루를 시작해요.")
      @Size(max = 100, message = "루틴 소개는 100자 이하여야 합니다.")
      String content,

      @Schema(description = "카드 목록 (1~10장, 배열 순서대로 저장)", requiredMode = Schema.RequiredMode.REQUIRED)
      @NotEmpty(message = "카드는 1장 이상이어야 합니다.")
      @Size(max = 10, message = "카드는 최대 10장까지 등록할 수 있습니다.")
      List<@Valid @NotNull(message = "카드 값이 비어 있습니다.") Card> cards
  ) {
  }

  @Schema(description = "루틴 카드")
  public record Card(
      @Schema(description = "카드 이미지 publicId (이미지 업로드 API 응답의 imageId)", example = DemoData.IMAGE_ID,
          requiredMode = Schema.RequiredMode.REQUIRED)
      @NotBlank(message = "카드 이미지는 필수입니다.")
      @Pattern(regexp = Ulid.REGEX, message = "올바른 이미지 ID 형식(ULID 26자)이 아닙니다.")
      String imageId,

      @Schema(description = "카드 제목 (5~30자)", example = "기상 후 물 한 잔 마시기", requiredMode = Schema.RequiredMode.REQUIRED)
      @NotBlank(message = "카드 제목은 필수입니다.")
      @Size(min = 5, max = 30, message = "카드 제목은 5자 이상 30자 이하여야 합니다.")
      String title,

      @Schema(description = "카드 내용 (선택, 최대 100자)", example = "미지근한 물 500ml")
      @Size(max = 100, message = "카드 내용은 100자 이하여야 합니다.")
      String content,

      @Schema(description = "시간 HH:mm (선택)", type = "string", pattern = "HH:mm", example = "07:00")
      @JsonFormat(pattern = "HH:mm")
      LocalTime time
  ) {
  }
}
