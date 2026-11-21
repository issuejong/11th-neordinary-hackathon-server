package com.example.server.global.auth;

import com.example.server.domain.user.repository.UserRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import com.example.server.global.util.Ulid;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 기기 토큰에 연결된 프로필을 확인하고 내부 사용자 ID를 반환한다.
 * X-User-Id는 해당 기기 안의 프로필 선택에 사용한다.
 * 유저 도메인(가입/로그인)이 완성되면 이 클래스를 실제 인증 방식으로 교체한다.
 */
@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

  private static final Pattern USER_ID_FORMAT = Pattern.compile(Ulid.REGEX);
  private static final PageRequest AT_MOST_TWO_USERS = PageRequest.of(0, 2);

  private final UserRepository userRepository;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(LoginUser.class)
        && Long.class.equals(parameter.getParameterType());
  }

  @Override
  public Long resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    String deviceToken = webRequest.getHeader(LoginUser.HEADER);
    String publicId = webRequest.getHeader(LoginUser.USER_ID_HEADER);
    LoginUser loginUser = parameter.getParameterAnnotation(LoginUser.class);
    if (deviceToken == null && publicId == null && loginUser != null && !loginUser.required()) {
      return null;
    }
    if (deviceToken == null || deviceToken.isBlank()) {
      throw new MissingRequestHeaderException(LoginUser.HEADER, parameter);
    }
    if (deviceToken.length() > 255) {
      throw new BusinessException(ErrorStatus._BAD_REQUEST);
    }
    if (publicId != null) {
      if (!USER_ID_FORMAT.matcher(publicId).matches()) {
        throw new BusinessException(ErrorStatus._BAD_REQUEST);
      }
      return userRepository.findActiveIdByDeviceTokenAndPublicId(deviceToken, publicId)
          .orElseThrow(() -> new BusinessException(ErrorStatus.USER_NOT_FOUND));
    }

    List<Long> ids = userRepository.findActiveIdsByDeviceToken(deviceToken, AT_MOST_TWO_USERS);
    if (ids.isEmpty()) {
      throw new BusinessException(ErrorStatus.USER_NOT_FOUND);
    }
    if (ids.size() > 1) {
      throw new BusinessException(ErrorStatus.USER_SELECTION_REQUIRED);
    }
    return ids.getFirst();
  }
}
