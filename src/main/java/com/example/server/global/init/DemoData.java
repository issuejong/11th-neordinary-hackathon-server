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

  /** 데모 사용자 publicId */
  public static final String USER_ID = "01KB0000000000000000MEM001";
  public static final String USER_NICKNAME = "데모 유저";
  /** 심사용 테스트 계정 슬롯 (데모 유저). Swagger 예시는 이 계정을 claim해서 실행한다. */
  public static final int REVIEW_SLOT = 8;

  /** 데모 참여자: 상세 루틴(RTN001)에 함께하기 결과를 공개한 사용자 (함께한 결과 조회 예시) */
  public static final String PARTICIPANT_ID = "01KB0000000000000000MEM002";

  /** 데모 작성자: 함께하기 예시 루틴의 소유자 (데모 유저는 본인 루틴에 참여할 수 없으므로 분리) */
  public static final String AUTHOR_ID = "01KB0000000000000000MEM003";

  /** 데모 유저가 참여 중인 루틴: 참여 시작 · 취소 · 초안 예시 */
  public static final String JOIN_ROUTINE_ID = "01KB0000000000000000RTN004";

  /** 데모 유저가 참여 중인 루틴: 초안 조회 · 저장 예시 (참여 취소 예시와 분리) */
  public static final String DRAFT_ROUTINE_ID = "01KB0000000000000000RTN008";

  /** 데모 유저가 참여 중인 루틴: 참여 완료(결과 제출) 예시 */
  public static final String COMPLETE_ROUTINE_ID = "01KB0000000000000000RTN005";

  /** 데모 유저가 결과를 공개한 루틴: 내 결과 수정 예시 */
  public static final String RESULT_EDIT_ROUTINE_ID = "01KB0000000000000000RTN006";

  /** 데모 유저가 결과를 공개한 루틴: 내 결과 삭제 예시 */
  public static final String RESULT_DELETE_ROUTINE_ID = "01KB0000000000000000RTN007";

  /** 상세 조회 예시용 루틴 */
  public static final String ROUTINE_ID = "01KB0000000000000000RTN001";

  /** Swagger 댓글 수정 · 삭제 예시 */
  public static final String COMMENT_ID = "01KB0000000000000000CMT001";

  /** 삭제 예시용 댓글 (수정 예시와 분리) */
  public static final String DELETABLE_COMMENT_ID = "01KB0000000000000000CMT002";

  /** 수정 예시용 루틴 (데모 사용자 소유) */
  public static final String EDITABLE_ROUTINE_ID = "01KB0000000000000000RTN002";

  /** 삭제 예시용 루틴 (수정 예시와 분리: 삭제 후에도 수정 예시가 동작하도록) */
  public static final String DELETABLE_ROUTINE_ID = "01KB0000000000000000RTN003";

  private DemoData() {
  }
}
