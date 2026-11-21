package com.example.server.domain.user.service.command;

import com.example.server.domain.image.entity.Image;
import com.example.server.domain.image.repository.ImageKey;
import com.example.server.domain.image.repository.ImageRepository;
import com.example.server.domain.image.repository.ProfileImageView;
import com.example.server.domain.image.support.ProfileImagePolicy;
import com.example.server.domain.user.converter.UserConverter;
import com.example.server.domain.user.dto.UserRequestDTO;
import com.example.server.domain.user.dto.UserResponseDTO;
import com.example.server.domain.user.entity.User;
import com.example.server.domain.user.repository.UserRepository;
import com.example.server.domain.user.service.query.UserQueryService;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.init.DemoData;
import com.example.server.global.response.code.status.ErrorStatus;
import com.example.server.global.util.Ulid;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

  private static final List<String> DEFAULT_PROFILE_IMAGE_IDS = List.of(
      DemoData.IMAGE_ID, DemoData.IMAGE_ID_2, DemoData.IMAGE_ID_3);

  private final UserRepository userRepository;
  private final ImageRepository imageRepository;
  private final ProfileImagePolicy profileImagePolicy;
  private final UserQueryService userQueryService;

  @Override
  @Transactional
  public UserResponseDTO create(String deviceToken, UserRequestDTO.Create request) {
    String nickname = request.nickname().strip();
    checkNicknameAvailability(nickname, null);

    List<ImageKey> images = imageRepository.findUsableKeys(DEFAULT_PROFILE_IMAGE_IDS, null);
    if (images.isEmpty()) {
      throw new BusinessException(ErrorStatus.IMAGE_NOT_FOUND);
    }
    ImageKey selected = images.get(ThreadLocalRandom.current().nextInt(images.size()));
    Image profileImage = imageRepository.getReferenceById(selected.id());
    User user = save(new User(Ulid.generate(), deviceToken, nickname, profileImage, normalizeBio(request.bio())));
    return UserConverter.toResponse(user, selected.publicId());
  }

  @Override
  @Transactional
  public UserResponseDTO updateProfile(Long userId, UserRequestDTO.UpdateProfile request) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BusinessException(ErrorStatus.USER_NOT_FOUND));
    if (request.getNickname() != null) {
      String nickname = request.getNickname().strip();
      if (!nickname.equals(user.getNickname())) {
        if (nickname.length() < 2 || nickname.length() > 10) {
          throw new BusinessException(ErrorStatus.USER_INVALID_NICKNAME);
        }
        checkNicknameAvailability(nickname, userId);
        user.changeNickname(nickname);
      }
    }
    if (request.hasBio()) {
      user.changeBio(normalizeBio(request.getBio()));
    }
    if (request.getProfileImagePublicId() != null) {
      ProfileImageView image = imageRepository.findUsableProfileImage(request.getProfileImagePublicId(), userId)
          .orElseThrow(() -> new BusinessException(ErrorStatus.IMAGE_NOT_FOUND));
      profileImagePolicy.validateSize(image.byteSize());
      profileImagePolicy.validateType(image.contentType());
      user.changeProfileImage(imageRepository.getReferenceById(image.id()));
    }
    save(user);
    return userQueryService.findMe(userId);
  }

  private void checkNicknameAvailability(String nickname, Long userId) {
    if (userRepository.existsOtherNickname(nickname, userId)) {
      throw new BusinessException(ErrorStatus.USER_DUPLICATE_NICKNAME);
    }
  }

  private User save(User user) {
    try {
      return userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      if (exception.getCause() instanceof ConstraintViolationException violation
          && violation.getConstraintName() != null
          && violation.getConstraintName().toLowerCase(Locale.ROOT).contains("uk_users_nickname_key")) {
        throw new BusinessException(ErrorStatus.USER_DUPLICATE_NICKNAME);
      }
      throw exception;
    }
  }

  private static String normalizeBio(String bio) {
    return bio == null || bio.isBlank() ? null : bio.strip();
  }
}
