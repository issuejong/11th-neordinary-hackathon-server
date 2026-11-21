package com.example.server.domain.image.service.command;

import com.example.server.domain.image.converter.ImageConverter;
import com.example.server.domain.image.dto.ImageResponseDTO;
import com.example.server.domain.image.entity.Image;
import com.example.server.domain.image.repository.ImageRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import com.example.server.global.util.Ulid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImageCommandServiceImpl implements ImageCommandService {

  /** iOS 카메라 기본 형식(heic/heif) 포함 */
  private static final Set<String> ALLOWED_TYPES = Set.of(
      "image/jpeg", "image/png", "image/heic", "image/heif", "image/webp", "image/gif");

  private final ImageRepository imageRepository;

  @Override
  @Transactional
  public ImageResponseDTO.Upload upload(Long userId, MultipartFile file) {
    if (file.isEmpty()) {
      throw new BusinessException(ErrorStatus.IMAGE_EMPTY);
    }
    String contentType = normalize(file.getContentType());
    if (!ALLOWED_TYPES.contains(contentType)) {
      throw new BusinessException(ErrorStatus.IMAGE_UNSUPPORTED_TYPE);
    }
    Image image = imageRepository.save(new Image(Ulid.generate(), contentType, readBytes(file), userId));
    return ImageConverter.toUploadResponse(image);
  }

  private static String normalize(String contentType) {
    if (contentType == null) {
      return "";
    }
    int semicolon = contentType.indexOf(';');
    String type = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
    type = type.trim().toLowerCase(Locale.ROOT);
    return "image/jpg".equals(type) ? "image/jpeg" : type;
  }

  private static byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
