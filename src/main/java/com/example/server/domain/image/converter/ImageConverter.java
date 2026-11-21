package com.example.server.domain.image.converter;

import com.example.server.domain.image.dto.ImageResponseDTO;
import com.example.server.domain.image.entity.Image;
import com.example.server.domain.image.support.ImageInspector.ImageInfo;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public final class ImageConverter {

  private ImageConverter() {
  }

  /** 현재 요청의 호스트 기준 절대 URL (운영: https://neo8.duckdns.org/api/images/{id}) */
  public static String toUrl(String publicId) {
    return ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/api/images/{imageId}")
        .buildAndExpand(publicId)
        .toUriString();
  }

  public static ImageResponseDTO.Upload toUploadResponse(Image image, ImageInfo info) {
    return new ImageResponseDTO.Upload(
        image.getPublicId(), image.getContentType(), image.getByteSize(), info.width(), info.height(),
        toUrl(image.getPublicId()));
  }

  public static ImageResponseDTO.File toFile(Image image) {
    return new ImageResponseDTO.File(image.getContentType(), image.getData());
  }
}
