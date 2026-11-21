package com.example.server.domain.image.service.command;

import com.example.server.domain.image.converter.ImageConverter;
import com.example.server.domain.image.dto.ImageResponseDTO;
import com.example.server.domain.image.entity.Image;
import com.example.server.domain.image.repository.ImageRepository;
import com.example.server.domain.image.support.ImageInspector;
import com.example.server.domain.image.support.ImageInspector.ImageInfo;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import com.example.server.global.util.Ulid;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 정책
 * - 용량: 파일 10MB (application.yml multipart 설정, 초과 시 FILE400)
 * - 형식: jpeg, png, heic, heif, webp, gif — 파일 내용(시그니처)으로 판별, 클라이언트 Content-Type은 무시
 * - 해상도: 가로 · 세로 각각 최대 8192px (아이폰 48MP 원본 8064x6048 허용)
 */
@Service
@RequiredArgsConstructor
public class ImageCommandServiceImpl implements ImageCommandService {

  public static final int MAX_DIMENSION = 8192;

  private final ImageRepository imageRepository;

  @Override
  @Transactional
  public ImageResponseDTO.Upload upload(Long userId, MultipartFile file) {
    if (file.isEmpty()) {
      throw new BusinessException(ErrorStatus.IMAGE_EMPTY);
    }
    byte[] data = readBytes(file);
    ImageInfo info = ImageInspector.inspect(data)
        .orElseThrow(() -> new BusinessException(ErrorStatus.IMAGE_UNSUPPORTED_TYPE));
    if (info.width() > MAX_DIMENSION || info.height() > MAX_DIMENSION) {
      throw new BusinessException(ErrorStatus.IMAGE_TOO_LARGE_DIMENSION);
    }
    Image image = imageRepository.save(new Image(Ulid.generate(), info.contentType(), data, userId));
    return ImageConverter.toUploadResponse(image, info);
  }

  private static byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
