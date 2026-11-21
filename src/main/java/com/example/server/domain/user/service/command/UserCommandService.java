package com.example.server.domain.user.service.command;

import com.example.server.domain.user.dto.UserRequestDTO;
import com.example.server.domain.user.dto.UserResponseDTO;

public interface UserCommandService {

  UserResponseDTO create(String nickname, String bio);

  UserResponseDTO updateProfile(Long userId, UserRequestDTO.UpdateProfile request);
}
