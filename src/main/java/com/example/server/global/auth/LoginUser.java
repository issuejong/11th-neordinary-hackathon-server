package com.example.server.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청한 사용자의 내부 id(users.id)를 컨트롤러 파라미터(Long)로 주입한다.
 * X-Device-Token으로 기기를 확인하고, X-User-Id로 그 기기의 프로필을 선택한다. 실제 로그인이 생기면
 * {@link LoginUserArgumentResolver}만 교체하면 되고 컨트롤러는 바꿀 필요가 없다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {

  /** 임시 인증 헤더 이름 */
  String HEADER = "X-Device-Token";

  String USER_ID_HEADER = "X-User-Id";

  /** false이면 헤더를 생략한 공개 조회에서 사용자 ID가 null로 주입된다. */
  boolean required() default true;

  /** Swagger 예시 토큰. 비어 있으면 기본 데모 사용자 토큰을 쓴다. */
  String example() default "";
}
