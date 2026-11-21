package com.example.server.domain.routine.repository.projection;

import com.example.server.domain.routine.entity.RoutineParticipation.Status;

public record RoutineInteractionView(long likeCount, long myLikeCount, long inProgressCount,
                                     long completedCount, Status myParticipationStatus) {
}
