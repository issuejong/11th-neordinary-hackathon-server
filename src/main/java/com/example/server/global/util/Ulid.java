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

  public static String generate() {
    char[] out = new char[26];
    long time = System.currentTimeMillis();
    for (int i = 9; i >= 0; i--) {
      out[i] = ALPHABET[(int) (time & 31)];
      time >>>= 5;
    }
    byte[] random = new byte[10];
    RANDOM.nextBytes(random);
    long hi = 0;
    long lo = 0;
    for (int i = 0; i < 5; i++) {
      hi = (hi << 8) | (random[i] & 0xFF);
      lo = (lo << 8) | (random[i + 5] & 0xFF);
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
