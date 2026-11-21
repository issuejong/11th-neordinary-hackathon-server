package com.example.server.domain.routine.repository.projection;

import java.time.LocalDateTime;

public record RoutineHeaderView(
    Long id,
    String publicId,
    String title,
    String content,
    String ownerPublicId,
    String ownerNickname,
    String ownerProfileImagePublicId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
