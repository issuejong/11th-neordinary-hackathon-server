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
  @DisplayName("단조 증가: 같은 밀리초에 연속 생성해도 생성 순서대로 정렬된다")
  void monotonic() {
    String prev = Ulid.generate();
    for (int i = 0; i < 10_000; i++) {
      String next = Ulid.generate();
      assertThat(next).isGreaterThan(prev);
      prev = next;
    }
  }

  @Test
  @DisplayName("같은 밀리초 연속 생성도 다음 값을 추측할 수 없다 (랜덤부 증가량이 1이 아니고 매번 다름)")
  void unpredictableWithinSameMillisecond() {
    String alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    java.util.function.ToLongFunction<String> lo = id -> {
      long v = 0;
      for (char c : id.substring(18).toCharArray()) {
        v = v * 32 + alphabet.indexOf(c);
      }
      return v;
    };
    Set<Long> steps = new HashSet<>();
    int sameMs = 0;
    String prev = Ulid.generate();
    for (int i = 0; i < 2_000; i++) {
      String next = Ulid.generate();
      if (next.substring(0, 18).equals(prev.substring(0, 18))) { // 같은 ms · 같은 상위 랜덤부
        long step = lo.applyAsLong(next) - lo.applyAsLong(prev);
        assertThat(step).isGreaterThan(1);
        steps.add(step);
        sameMs++;
      }
      prev = next;
    }
    assertThat(sameMs).as("같은 ms 생성이 충분히 발생").isGreaterThan(100);
    assertThat(steps).as("증가량이 매번 달라 추측 불가").hasSizeGreaterThan(sameMs * 9 / 10);
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
