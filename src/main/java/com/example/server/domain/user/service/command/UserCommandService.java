package com.example.server.domain.user.service.command;

import com.example.server.domain.user.dto.UserRequestDTO;
import com.example.server.domain.user.dto.UserResponseDTO;

public interface UserCommandService {

  UserResponseDTO create(String deviceToken, UserRequestDTO.Create request);
}
