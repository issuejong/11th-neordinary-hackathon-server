package com.example.server.global.util;

import java.security.SecureRandom;

/** 외부 노출용 식별자(ULID, Crockford Base32 26자) 생성기. */
public final class Ulid {

  /** @Pattern 검증용 정규식 (컴파일 타임 상수) */
  public static final String REGEX = "^[0-9A-HJKMNP-TV-Z]{26}$";

  private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
  private static final SecureRandom RANDOM = new SecureRandom();

  private Ulid() {
  }

  private static long lastTime = -1;
  private static long lastHi;
  private static long lastLo;

  /**
   * 단조 증가 ULID: 같은 밀리초 안에서는 직전 값의 랜덤부를 1 올려서, 생성 순서 = 정렬 순서가 되게 한다.
   * (루틴 목록 최신순 · 커서가 publicId 순서를 쓰므로 필요)
   */
  public static synchronized String generate() {
    long time = Math.max(System.currentTimeMillis(), lastTime);
    long hi;
    long lo;
    if (time == lastTime) {
      hi = lastHi;
      lo = (lastLo + 1) & MASK_40;
      if (lo == 0) {
        hi = (hi + 1) & MASK_40;
      }
    } else {
      byte[] random = new byte[10];
      RANDOM.nextBytes(random);
      hi = 0;
      lo = 0;
      for (int i = 0; i < 5; i++) {
        hi = (hi << 8) | (random[i] & 0xFF);
        lo = (lo << 8) | (random[i + 5] & 0xFF);
      }
    }
    lastTime = time;
    lastHi = hi;
    lastLo = lo;
    return encode(time, hi, lo);
  }

  private static final long MASK_40 = (1L << 40) - 1;

  private static String encode(long time, long hi, long lo) {
    char[] out = new char[26];
    for (int i = 9; i >= 0; i--) {
      out[i] = ALPHABET[(int) (time & 31)];
      time >>>= 5;
    }
    for (int i = 17; i >= 10; i--) {
      out[i] = ALPHABET[(int) (hi & 31)];
      hi >>>= 5;
    }
    for (int i = 25; i >= 18; i--) {
      out[i] = ALPHABET[(int) (lo & 31)];
      lo >>>= 5;
    }
    return new String(out);
  }
}
