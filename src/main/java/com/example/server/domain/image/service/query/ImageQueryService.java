package com.example.server.domain.image.service.query;

import com.example.server.domain.image.dto.ImageResponseDTO;

public interface ImageQueryService {

  ImageResponseDTO.File getFile(String imageId);
}
