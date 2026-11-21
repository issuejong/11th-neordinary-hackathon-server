package com.example.server.domain.user.service.query;

import com.example.server.domain.user.dto.UserResponseDTO;
import com.example.server.domain.user.repository.UserRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

  private final UserRepository userRepository;

  @Override
  public UserResponseDTO findMe(Long userId) {
    return userRepository.findActiveProfileById(userId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.USER_NOT_FOUND));
  }
}
