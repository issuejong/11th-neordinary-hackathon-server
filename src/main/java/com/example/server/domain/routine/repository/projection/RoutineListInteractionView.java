package com.example.server.domain.routine.repository.projection;

public record RoutineListInteractionView(String routineId, long myLikeCount, long inProgressCount,
                                         long completedCount, long myParticipationCount, long myCompletedCount) {
}
