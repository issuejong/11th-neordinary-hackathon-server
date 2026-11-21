package com.example.server.domain.user.support;

import com.example.server.global.exception.BusinessException;
import com.example.server.global.response.code.status.ErrorStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class TestAccountTokens {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Pattern BEARER = Pattern.compile("Bearer ([A-Za-z0-9_-]{43})", Pattern.CASE_INSENSITIVE);

  private TestAccountTokens() {
  }

  public static String generate() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hashAuthorization(String authorization) {
    if (authorization == null) {
      throw new BusinessException(ErrorStatus.TEST_SESSION_INVALID);
    }
    var matcher = BEARER.matcher(authorization);
    if (!matcher.matches()) {
      throw new BusinessException(ErrorStatus.TEST_SESSION_INVALID);
    }
    return hash(matcher.group(1));
  }

  public static String hash(String token) {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
