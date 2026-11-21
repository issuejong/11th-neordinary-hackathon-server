package com.example.server.domain.user.service.query;

import com.example.server.domain.user.dto.UserResponseDTO;
import java.util.List;

public interface UserQueryService {

  List<UserResponseDTO> findByDeviceToken(String deviceToken);

  UserResponseDTO findMe(Long userId);
}
