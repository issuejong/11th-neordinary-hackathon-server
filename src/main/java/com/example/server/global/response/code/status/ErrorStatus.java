package com.example.server.global.response.code.status;

import com.example.server.global.response.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseCode {
  // 공통
  _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "요청 형식이 올바르지 않습니다."),
  _NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "리소스를 찾을 수 없습니다."),
  _METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405", "지원하지 않는 HTTP 메서드입니다."),
  _UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON415", "지원하지 않는 Content-Type입니다."),
  _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다."),

  // 파일 업로드
  _FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "FILE400", "파일 크기가 허용 한도(파일 10MB, 요청 20MB)를 초과했습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
