package com.example.server.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청한 사용자의 내부 id(users.id)를 컨트롤러 파라미터(Long)로 주입한다.
 * 현재는 임시로 X-Device-Token 헤더(users.device_token, 응답에 노출되지 않는 기기별 비밀값)를 사용한다. 실제 로그인이 생기면
 * {@link LoginUserArgumentResolver}만 교체하면 되고 컨트롤러는 바꿀 필요가 없다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {

  /** 임시 인증 헤더 이름 */
  String HEADER = "X-Device-Token";
}
