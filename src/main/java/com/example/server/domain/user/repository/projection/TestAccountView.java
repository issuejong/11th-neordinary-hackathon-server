package com.example.server.domain.user.repository.projection;

import java.time.Instant;

public record TestAccountView(Integer slot, String publicId, String nickname, String profileImagePublicId,
                              Instant expiresAt) {
}
