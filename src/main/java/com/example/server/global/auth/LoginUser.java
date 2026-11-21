package com.example.server.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.HttpHeaders;

/**
 * 요청한 사용자의 내부 id(users.id)를 컨트롤러 파라미터(Long)로 주입한다.
 * 테스트 계정 점유 세션으로 사용자를 확인한다. 실제 로그인이 생기면
 * {@link LoginUserArgumentResolver}만 교체하면 되고 컨트롤러는 바꿀 필요가 없다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {

  String HEADER = HttpHeaders.AUTHORIZATION;

  /** false이면 헤더를 생략한 공개 조회에서 사용자 ID가 null로 주입된다. */
  boolean required() default true;

}
