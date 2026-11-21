package com.example.server.global.init;

/**
 * 서버 시작 시 자동으로 넣는 데모/기본 데이터의 고정 publicId.
 * Swagger 예시값으로 쓰여서 심사자가 값 입력 없이 바로 API를 실행할 수 있다.
 */
public final class DemoData {

  /** 기본 프로필 이미지 (시스템 등록, created_by = NULL) */
  public static final String IMAGE_ID = "01KB0000000000000000DEF001";
  public static final String IMAGE_ID_2 = "01KB0000000000000000DEF002";
  public static final String IMAGE_ID_3 = "01KB0000000000000000DEF003";

  /** 데모 사용자 (USER_DEVICE_TOKEN이 임시 인증 X-Device-Token 예시값) */
  public static final String USER_ID = "01KB0000000000000000MEM001";
  public static final String USER_DEVICE_TOKEN = "demo-device-token";

  /** 상세 조회 예시용 루틴 */
  public static final String ROUTINE_ID = "01KB0000000000000000RTN001";

  /** Swagger 댓글 수정 · 삭제 예시 */
  public static final String COMMENT_ID = "01KB0000000000000000CMT001";

  /** 수정 · 삭제 예시용 루틴 (데모 사용자 소유) */
  public static final String EDITABLE_ROUTINE_ID = "01KB0000000000000000RTN002";

  private DemoData() {
  }
}
