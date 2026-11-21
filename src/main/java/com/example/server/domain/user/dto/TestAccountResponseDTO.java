package com.example.server.domain.user.dto;

import java.time.Instant;

public class TestAccountResponseDTO {

  public record Account(int slot, String publicId, String nickname, String profileImagePublicId, boolean available) {
  }

  public record Claim(String publicId, String accessToken, Instant expiresAt) {
  }

  public record Heartbeat(Instant expiresAt) {
  }
}
