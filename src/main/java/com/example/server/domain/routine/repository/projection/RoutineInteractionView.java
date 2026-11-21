package com.example.server.domain.routine.repository.projection;

public record RoutineInteractionView(long likeCount, long myLikeCount, long inProgressCount,
                                     long completedCount, long myParticipationCount, long myCompletedCount) {
}
