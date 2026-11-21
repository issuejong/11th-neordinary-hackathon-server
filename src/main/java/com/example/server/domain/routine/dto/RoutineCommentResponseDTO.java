package com.example.server.domain.routine.dto;

import java.time.LocalDateTime;

public record RoutineCommentResponseDTO(String commentId, String authorNickname, String profileImageUrl,
                                        String content, LocalDateTime createdAt, boolean isMine) {
}
