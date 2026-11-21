package com.example.server.global.auth;

import com.example.server.domain.user.repository.TestAccountLeaseRepository;
import com.example.server.domain.user.support.TestAccountTokens;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 테스트 계정 점유 세션을 활성 프로필의 내부 사용자 ID로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

  private final TestAccountLeaseRepository leases;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(LoginUser.class)
        && Long.class.equals(parameter.getParameterType());
  }

  @Override
  public Long resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    String authorization = webRequest.getHeader(LoginUser.HEADER);
    LoginUser loginUser = parameter.getParameterAnnotation(LoginUser.class);
    if (authorization == null && loginUser != null && !loginUser.required()) {
      return null;
    }
    String tokenHash = TestAccountTokens.hashAuthorization(authorization);
    return leases.findActiveUserId(tokenHash, Instant.now())
        .orElseThrow(() -> new BusinessException(ErrorStatus.TEST_SESSION_INVALID));
  }
}
