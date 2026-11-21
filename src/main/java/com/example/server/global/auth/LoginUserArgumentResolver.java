package com.example.server.global.auth;

import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import com.example.server.global.util.Ulid;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * [임시] X-User-Id 헤더(users.public_id) → users.id.
 * 유저 도메인(가입/로그인)이 완성되면 이 클래스를 실제 인증 방식으로 교체한다.
 */
@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

  private final EntityManager entityManager;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(LoginUser.class)
        && Long.class.equals(parameter.getParameterType());
  }

  @Override
  public Long resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    String publicId = webRequest.getHeader(LoginUser.HEADER);
    if (publicId == null || publicId.isBlank()) {
      throw new MissingRequestHeaderException(LoginUser.HEADER, parameter);
    }
    if (!publicId.matches(Ulid.REGEX)) {
      throw new BusinessException(ErrorStatus._BAD_REQUEST);
    }
    List<Long> ids = entityManager.createQuery(
            "select u.id from AppUser u where u.publicId = :publicId and u.deletedAt is null", Long.class)
        .setParameter("publicId", publicId)
        .getResultList();
    if (ids.isEmpty()) {
      throw new BusinessException(ErrorStatus.USER_NOT_FOUND);
    }
    return ids.getFirst();
  }
}
