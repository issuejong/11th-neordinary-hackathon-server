package com.example.server.domain.routine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineLike;
import com.example.server.domain.routine.entity.RoutineParticipation;
import com.example.server.domain.user.entity.User;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.support.ApiTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RoutineInteractionApiTests extends ApiTestSupport {

  @Test
  void returnsActiveCountsAndViewerState() throws Exception {
    String routineId = createRoutine(DEMO_USER_ID);
    String viewer = newUser();
    String completed = newUser();
    String canceled = newUser();

    tx.executeWithoutResult(status -> {
      Routine routine = entityManager.createQuery("from Routine r where r.publicId = :publicId", Routine.class)
          .setParameter("publicId", routineId).getSingleResult();
      User viewerUser = user(viewer);
      User completedUser = user(completed);
      User canceledUser = user(canceled);
      LocalDateTime now = LocalDateTime.now();

      entityManager.persist(new RoutineLike(routine, viewerUser));
      RoutineLike canceledLike = new RoutineLike(routine, canceledUser);
      canceledLike.markDeleted(canceledUser.getId(), now);
      entityManager.persist(canceledLike);

      entityManager.persist(new RoutineParticipation(routine, viewerUser, now));
      RoutineParticipation completedParticipation = new RoutineParticipation(routine, completedUser, now);
      completedParticipation.saveWork(participationResult(2));
      completedParticipation.complete(now);
      entityManager.persist(completedParticipation);
      RoutineParticipation canceledParticipation = new RoutineParticipation(routine, canceledUser, now);
      canceledParticipation.markDeleted(canceledUser.getId(), now);
      entityManager.persist(canceledParticipation);
    });

    String path = "/api/routines/" + routineId + "/interactions";
    mvc.perform(get(path).header(LoginUser.HEADER, authorization(viewer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.likeCount").value(1))
        .andExpect(jsonPath("$.result.likedByMe").value(true))
        .andExpect(jsonPath("$.result.inProgressCount").value(1))
        .andExpect(jsonPath("$.result.completedCount").value(1))
        .andExpect(jsonPath("$.result.myParticipationStatus").value("IN_PROGRESS"));
    mvc.perform(get(path).header(LoginUser.HEADER, authorization(completed)))
        .andExpect(jsonPath("$.result.myParticipationStatus").value("COMPLETED"));
    mvc.perform(get(path).header(LoginUser.HEADER, authorization(canceled)))
        .andExpect(jsonPath("$.result.likedByMe").value(false))
        .andExpect(jsonPath("$.result.myParticipationStatus").value("NOT_PARTICIPATING"));
  }

  @Test
  void validatesRoutineIdAndRequiresSession() throws Exception {
    mvc.perform(get("/api/routines/" + DemoData.ROUTINE_ID + "/interactions"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/routines/invalid/interactions").header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/routines/" + UNKNOWN_ID + "/interactions").header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROUTINE404"));
  }

  private User user(String token) {
    return entityManager.createQuery("from AppUser u where u.publicId = :token", User.class)
        .setParameter("token", token).getSingleResult();
  }
}
