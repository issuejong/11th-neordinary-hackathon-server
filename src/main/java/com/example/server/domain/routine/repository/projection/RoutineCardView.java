package com.example.server.domain.routine.repository.projection;

import java.time.LocalTime;

public record RoutineCardView(
    String publicId,
    int sortOrder,
    String title,
    String content,
    LocalTime cardTime,
    String imagePublicId
) {
}
