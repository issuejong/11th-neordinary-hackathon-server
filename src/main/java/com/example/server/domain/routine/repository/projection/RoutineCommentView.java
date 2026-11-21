package com.example.server.domain.routine.repository.projection;

import java.time.LocalDateTime;

public record RoutineCommentView(String publicId, Long authorId, String authorNickname,
                                 String profileImagePublicId, String content, LocalDateTime createdAt) {
}
