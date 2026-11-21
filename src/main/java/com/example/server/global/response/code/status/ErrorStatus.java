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
  _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "권한이 없습니다."),
  _NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "리소스를 찾을 수 없습니다."),
  _METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405", "지원하지 않는 HTTP 메서드입니다."),
  _CONFLICT(HttpStatus.CONFLICT, "COMMON409", "이미 처리된 요청이거나 데이터가 충돌합니다."),
  _UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON415", "지원하지 않는 Content-Type입니다."),
  _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다."),

  // 파일 업로드
  _FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "FILE400", "파일 크기가 허용 한도(파일 10MB, 요청 20MB)를 초과했습니다."),

  // 사용자
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "사용자를 찾을 수 없습니다."),
  USER_INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "USER4001", "닉네임은 2자 이상 10자 이하여야 합니다."),
  USER_DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER4091", "중복된 이름입니다. 다른 이름을 입력해 주세요."),
  TEST_SESSION_INVALID(HttpStatus.UNAUTHORIZED, "TEST401", "테스트 계정 사용권이 없거나 만료됐습니다."),
  TEST_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "TEST404", "등록된 테스트 계정을 찾을 수 없습니다."),
  TEST_ACCOUNT_IN_USE(HttpStatus.CONFLICT, "TEST4091", "다른 사람이 이 테스트 계정을 사용 중입니다."),
  TEST_SLOT_OCCUPIED(HttpStatus.CONFLICT, "TEST4092", "이 위치에 이미 테스트 계정이 등록되어 있습니다."),

  // 이미지
  IMAGE_EMPTY(HttpStatus.BAD_REQUEST, "IMAGE4001", "빈 파일은 업로드할 수 없습니다."),
  IMAGE_UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "IMAGE4002", "지원하지 않는 이미지 형식이거나 손상된 파일입니다. (jpg, png, heic, heif, webp, gif)"),
  IMAGE_TOO_LARGE_DIMENSION(HttpStatus.BAD_REQUEST, "IMAGE4003", "이미지 해상도가 허용 범위(가로 · 세로 최대 픽셀)를 초과했습니다."),
  PROFILE_IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMAGE4004", "프로필 이미지는 허용된 파일 크기 이하여야 합니다. 기본 한도는 5MB입니다."),
  PROFILE_IMAGE_UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "IMAGE4005", "프로필 이미지는 jpg, png, heic, heif 형식만 사용할 수 있습니다."),
  IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE404", "이미지를 찾을 수 없습니다."),

  // 루틴
  STEP_IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMAGE4006", "스텝 이미지는 최대 5MB입니다."),
  STEP_IMAGE_UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "IMAGE4007", "스텝 이미지는 JPEG, PNG, HEIC, HEIF만 지원합니다."),
  ROUTINE_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "ROUTINE4001", "잘못된 커서입니다. 이전 응답의 nextCursor를 같은 sort와 함께 보내 주세요."),
  ROUTINE_FORBIDDEN(HttpStatus.FORBIDDEN, "ROUTINE403", "루틴 작성자만 수정하거나 삭제할 수 있습니다."),
  ROUTINE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROUTINE404", "루틴을 찾을 수 없습니다."),

  // 댓글
  COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT403", "댓글 작성자만 수정하거나 삭제할 수 있습니다."),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT404", "댓글을 찾을 수 없습니다."),

  // 참여
  PARTICIPATION_OWNER_FORBIDDEN(HttpStatus.FORBIDDEN, "PARTICIPATION403", "내가 만든 루틴에는 참여할 수 없습니다."),
  PARTICIPATION_INVALID_STATE(HttpStatus.CONFLICT, "PARTICIPATION409", "현재 참여 상태에서는 이 동작을 할 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
