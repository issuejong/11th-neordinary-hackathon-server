package com.example.server.domain.routine.dto;

import java.util.Locale;

/** 루틴 목록 정렬 (쿼리 스트링 sort=latest|likes|participants, 기본 latest) */
public enum RoutineSort {
  LATEST,
  LIKES,
  PARTICIPANTS;

  /** 쿼리 스트링 검증용 정규식 (대소문자 무시) */
  public static final String REGEX = "(?i)latest|likes|participants";

  public static RoutineSort from(String value) {
    return valueOf(value.toUpperCase(Locale.ROOT));
  }
}
