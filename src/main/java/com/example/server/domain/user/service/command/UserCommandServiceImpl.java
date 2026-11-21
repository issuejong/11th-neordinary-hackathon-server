package com.example.server.domain.user.service.command;

import com.example.server.domain.image.entity.Image;
import com.example.server.domain.image.repository.ImageKey;
import com.example.server.domain.image.repository.ImageRepository;
import com.example.server.domain.user.converter.UserConverter;
import com.example.server.domain.user.dto.UserRequestDTO;
import com.example.server.domain.user.dto.UserResponseDTO;
import com.example.server.domain.user.entity.User;
import com.example.server.domain.user.repository.UserRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.init.DemoData;
import com.example.server.global.response.code.status.ErrorStatus;
import com.example.server.global.util.Ulid;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

  private static final List<String> DEFAULT_PROFILE_IMAGE_IDS = List.of(
      DemoData.IMAGE_ID, DemoData.IMAGE_ID_2, DemoData.IMAGE_ID_3);

  private final UserRepository userRepository;
  private final ImageRepository imageRepository;

  @Override
  @Transactional
  public UserResponseDTO create(String deviceToken, UserRequestDTO.Create request) {
    if (userRepository.existsByDeviceToken(deviceToken)) {
      throw new BusinessException(ErrorStatus._CONFLICT);
    }

    List<ImageKey> images = imageRepository.findUsableKeys(DEFAULT_PROFILE_IMAGE_IDS, null);
    if (images.isEmpty()) {
      throw new BusinessException(ErrorStatus.IMAGE_NOT_FOUND);
    }
    ImageKey selected = images.get(ThreadLocalRandom.current().nextInt(images.size()));
    Image profileImage = imageRepository.getReferenceById(selected.id());
    String bio = request.bio() == null || request.bio().isBlank() ? null : request.bio().strip();
    User user = userRepository.save(new User(Ulid.generate(), deviceToken, request.nickname().strip(), profileImage, bio));
    return UserConverter.toResponse(user, selected.publicId());
  }
}
