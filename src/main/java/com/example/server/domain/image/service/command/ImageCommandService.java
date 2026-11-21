package com.example.server.domain.image.service.command;

import com.example.server.domain.image.dto.ImageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ImageCommandService {

  ImageResponseDTO.Upload upload(Long userId, MultipartFile file);

  ImageResponseDTO.Upload uploadProfile(Long userId, MultipartFile file);
  ImageResponseDTO.Upload uploadStep(Long userId, MultipartFile file);
}
