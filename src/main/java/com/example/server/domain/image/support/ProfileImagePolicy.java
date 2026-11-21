package com.example.server.domain.image.support;

import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImagePolicy {

  private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/heic", "image/heif");

  private final ImageProperties imageProperties;

  public void validateSize(long byteSize) {
    if (byteSize > imageProperties.profileMaxSize().toBytes()) {
      throw new BusinessException(ErrorStatus.PROFILE_IMAGE_TOO_LARGE);
    }
  }

  public void validateType(String contentType) {
    if (!TYPES.contains(contentType)) {
      throw new BusinessException(ErrorStatus.PROFILE_IMAGE_UNSUPPORTED_TYPE);
    }
  }
}
