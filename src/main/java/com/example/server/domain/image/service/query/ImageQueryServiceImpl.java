package com.example.server.domain.image.service.query;

import com.example.server.domain.image.converter.ImageConverter;
import com.example.server.domain.image.dto.ImageResponseDTO;
import com.example.server.domain.image.repository.ImageRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageQueryServiceImpl implements ImageQueryService {

  private final ImageRepository imageRepository;

  @Override
  @Transactional(readOnly = true)
  public ImageResponseDTO.File getFile(String imageId) {
    return imageRepository.findByPublicIdAndDeletedAtIsNull(imageId)
        .map(ImageConverter::toFile)
        .orElseThrow(() -> new BusinessException(ErrorStatus.IMAGE_NOT_FOUND));
  }
}
