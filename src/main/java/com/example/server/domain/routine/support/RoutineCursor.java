package com.example.server.domain.routine.support;

import com.example.server.domain.routine.dto.RoutineSort;
import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import com.example.server.global.util.Ulid;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 루틴 목록 커서. 마지막으로 받은 항목의 (정렬 값, publicId)를 Base64URL 문자열로 감싼다.
 * - 내부 PK(id)는 노출하지 않고, 시간순 정렬되는 publicId(ULID)를 동점 처리 · 최신순 기준으로 쓴다.
 * - 정렬 종류도 함께 넣어서 다른 정렬의 커서를 쓰면 거절한다.
 */
public record RoutineCursor(RoutineSort sort, long count, String publicId) {

  /** 첫 페이지: 모든 ULID · 모든 카운트보다 큰 값 */
  private static final String MAX_PUBLIC_ID = "Z".repeat(26);

  public static RoutineCursor first(RoutineSort sort) {
    return new RoutineCursor(sort, Long.MAX_VALUE, MAX_PUBLIC_ID);
  }

  public static RoutineCursor after(RoutineSort sort, long count, String publicId) {
    return new RoutineCursor(sort, sort == RoutineSort.LATEST ? 0 : count, publicId);
  }

  public String encode() {
    String raw = sort.name() + "|" + count + "|" + publicId;
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  /** cursor가 없으면 첫 페이지. 형식이 틀리거나 정렬이 다르면 400 */
  public static RoutineCursor decode(String cursor, RoutineSort sort) {
    if (cursor == null || cursor.isBlank()) {
      return first(sort);
    }
    try {
      String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|", -1);
      if (parts.length != 3 || !parts[0].equals(sort.name()) || !parts[2].matches(Ulid.REGEX)) {
        throw new BusinessException(ErrorStatus.ROUTINE_INVALID_CURSOR);
      }
      long count = Long.parseLong(parts[1]);
      if (count < 0) {
        throw new BusinessException(ErrorStatus.ROUTINE_INVALID_CURSOR);
      }
      return new RoutineCursor(sort, count, parts[2]);
    } catch (IllegalArgumentException e) { // Base64 · 숫자 형식 오류
      throw new BusinessException(ErrorStatus.ROUTINE_INVALID_CURSOR);
    }
  }
}
