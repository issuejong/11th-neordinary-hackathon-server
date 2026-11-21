package com.example.server.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ImageResponseDTO {

  @Schema(description = "이미지 업로드 결과")
  public record Upload(
      @Schema(description = "이미지 publicId (루틴 카드 생성 시 imageId로 사용)", example = "01KB0000000000000000DEF001")
      String imageId,
      @Schema(description = "파일 내용으로 판별한 MIME 타입", example = "image/jpeg")
      String contentType,
      @Schema(description = "파일 크기(byte)", example = "524288")
      int byteSize,
      @Schema(description = "가로(px)", example = "2048")
      int width,
      @Schema(description = "세로(px)", example = "1536")
      int height,
      @Schema(description = "이미지 조회 URL", example = "https://neo8.duckdns.org/api/images/01KB0000000000000000DEF001")
      String imageUrl
  ) {
  }

  /** 이미지 조회(바이너리 응답)용 내부 DTO */
  public record File(String contentType, byte[] data) {
  }
}
