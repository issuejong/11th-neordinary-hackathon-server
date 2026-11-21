package com.example.server.global.auth;

import com.example.server.domain.user.repository.UserRepository;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * [임시] X-Device-Token 헤더(users.device_token) → users.id.
 * publicId는 응답에 노출되므로 인증에 쓰지 않는다. 기기 토큰은 응답에 절대 포함하지 않는다.
 * 유저 도메인(가입/로그인)이 완성되면 이 클래스를 실제 인증 방식으로 교체한다.
 */
@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

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
    LoginUser loginUser = parameter.getParameterAnnotation(LoginUser.class);
    if (deviceToken == null && loginUser != null && !loginUser.required()) {
      return null;
    }
    if (deviceToken == null || deviceToken.isBlank()) {
      throw new MissingRequestHeaderException(LoginUser.HEADER, parameter);
    }
    if (deviceToken.length() > 255) {
      throw new BusinessException(ErrorStatus._BAD_REQUEST);
    }
    return userRepository.findActiveIdByDeviceToken(deviceToken)
        .orElseThrow(() -> new BusinessException(ErrorStatus.USER_NOT_FOUND));
  }
}
