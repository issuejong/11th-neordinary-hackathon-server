package com.example.server.domain.user.service.query;

import com.example.server.domain.user.dto.UserResponseDTO;

public interface UserQueryService {

  UserResponseDTO findMe(Long userId);

  UserResponseDTO findProfile(String publicId);
}
