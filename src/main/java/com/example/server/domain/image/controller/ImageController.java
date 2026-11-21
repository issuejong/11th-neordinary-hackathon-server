package com.example.server.domain.image.controller;

import com.example.server.domain.image.dto.ImageResponseDTO;
import com.example.server.domain.image.service.command.ImageCommandService;
import com.example.server.domain.image.service.query.ImageQueryService;
import com.example.server.domain.image.support.ImageProperties;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.global.response.ApiResponse;
import com.example.server.global.response.code.status.SuccessStatus;
import com.example.server.global.util.Ulid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Image")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

  private final ImageCommandService commandService;
  private final ImageQueryService queryService;
  private final ImageProperties imageProperties;

  @Operation(summary = "이미지 업로드",
      description = """
          - 형식: jpg, png, heic, heif, webp, gif (파일 내용으로 판별)
          - 용량: 최대 10MB / 해상도: 가로 · 세로 최대 8192px
          - 응답의 imageId를 루틴 스텝(카드) 생성에 사용합니다.
          - 권장: 앱에서 긴 변 2048px, JPEG 품질 0.8 정도로 줄여서 업로드
          """)
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ImageResponseDTO.Upload>> upload(
      @LoginUser Long userId,
      @Parameter(description = "업로드할 이미지 파일") @RequestPart("file") MultipartFile file
  ) {
    ImageResponseDTO.Upload result = commandService.upload(userId, file);
    return ResponseEntity.created(URI.create(result.imageUrl()))
        .body(ApiResponse.of(SuccessStatus._CREATED, result));
  }

  @Operation(summary = "이미지 조회", description = "이미지 바이너리를 그대로 반환합니다. (img 태그 / AsyncImage에서 바로 사용)",
      responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 파일",
          content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "image/*",
              schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary"))))
  @GetMapping("/{imageId}")
  public ResponseEntity<byte[]> find(
      @Parameter(description = "이미지 publicId", example = DemoData.IMAGE_ID)
      @PathVariable @Pattern(regexp = Ulid.REGEX, message = Ulid.MESSAGE) String imageId
  ) {
    ImageResponseDTO.File file = queryService.getFile(imageId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .cacheControl(CacheControl.maxAge(imageProperties.cacheMaxAge()).cachePublic().immutable())
        .header("X-Content-Type-Options", "nosniff")
        .body(file.data());
  }
}
