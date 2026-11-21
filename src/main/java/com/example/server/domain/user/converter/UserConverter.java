package com.example.server.domain.user.converter;

import com.example.server.domain.user.dto.UserResponseDTO;
import com.example.server.domain.user.entity.User;

public final class UserConverter {

  private UserConverter() {
  }

  public static UserResponseDTO toResponse(User user, String profileImagePublicId) {
    return new UserResponseDTO(user.getPublicId(), user.getNickname(), profileImagePublicId);
  }
}
