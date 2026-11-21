package com.example.server.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UlidTests {

  @Test
  @DisplayName("26자 Crockford Base32 형식")
  void format() {
    for (int i = 0; i < 1000; i++) {
      assertThat(Ulid.generate()).hasSize(26).matches(Ulid.REGEX);
    }
  }

  @Test
  @DisplayName("1만 개 생성해도 중복 없음")
  void unique() {
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < 10_000; i++) {
      ids.add(Ulid.generate());
    }
    assertThat(ids).hasSize(10_000);
  }

  @Test
  @DisplayName("앞 10자리(시간)는 시간순으로 증가한다")
  void timeOrdered() throws InterruptedException {
    String first = Ulid.generate();
    Thread.sleep(5);
    String second = Ulid.generate();
    assertThat(second.substring(0, 10)).isGreaterThan(first.substring(0, 10));
  }

  @Test
  @DisplayName("시간 부분이 현재 시각을 정확히 인코딩한다")
  void encodesCurrentTime() {
    long before = System.currentTimeMillis();
    String id = Ulid.generate();
    long after = System.currentTimeMillis();
    String alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    long decoded = 0;
    for (char c : id.substring(0, 10).toCharArray()) {
      decoded = decoded * 32 + alphabet.indexOf(c);
    }
    assertThat(decoded).isBetween(before, after);
  }
}
