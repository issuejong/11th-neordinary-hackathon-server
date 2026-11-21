package com.example.server.global.util;

/** 여러 DTO가 함께 쓰는 문자열 검증 규칙 (@Pattern용 컴파일 타임 상수) */
public final class TextPatterns {

  /** 줄바꿈(모든 개행 문자) 없는 한 줄 텍스트 */
  public static final String SINGLE_LINE = "[^\\r\\n\\x0B\\f\\x85\\u2028\\u2029]*";

  public static final String SINGLE_LINE_MESSAGE = "줄바꿈을 넣을 수 없습니다.";

  private TextPatterns() {
  }
}
