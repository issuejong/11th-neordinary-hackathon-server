package com.example.server.domain.routine.repository.projection;

import java.time.LocalDateTime;

public record RoutineSummaryView(
    String publicId,
    String title,
    String content,
    String ownerPublicId,
    String ownerNickname,
    String ownerProfileImagePublicId,
    String thumbnailImagePublicId,
    long cardCount,
    long likeCount,
    long participantCount,
    LocalDateTime createdAt
) {
}
