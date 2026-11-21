package com.example.server.global.response.code.status;

import com.example.server.global.response.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseCode {
  _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "요청 형식이 올바르지 않습니다."),
  _NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "리소스를 찾을 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
