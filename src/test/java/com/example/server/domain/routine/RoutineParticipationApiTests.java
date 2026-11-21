package com.example.server.domain.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.domain.routine.entity.RoutineParticipation;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.support.ApiTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RoutineParticipationApiTests extends ApiTestSupport {

  @Test
  void startsCancelsRestartsAndCompletesParticipation() throws Exception {
    String routineId = createRoutine(DEMO_TOKEN);
    String token = newUser();
    String base = "/api/routines/" + routineId;
    String path = base + "/participation";

    mvc.perform(put(path + "/completion").header(LoginUser.HEADER, token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PARTICIPATION409"));
    mvc.perform(put(path).header(LoginUser.HEADER, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.inProgressCount").value(1))
        .andExpect(jsonPath("$.result.myParticipationStatus").value("IN_PROGRESS"));
    RoutineParticipation original = readParticipation(routineId, token);
    mvc.perform(put(path).header(LoginUser.HEADER, token))
        .andExpect(jsonPath("$.result.inProgressCount").value(1));
    assertThat(readParticipation(routineId, token).getJoinedAt()).isEqualTo(original.getJoinedAt());

    mvc.perform(delete(path).header(LoginUser.HEADER, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.inProgressCount").value(0))
        .andExpect(jsonPath("$.result.myParticipationStatus").value("NOT_PARTICIPATING"));
    mvc.perform(delete(path).header(LoginUser.HEADER, token))
        .andExpect(status().isOk());
    assertThat(readParticipation(routineId, token).getDeletedAt()).isNotNull();

    // 이전 데이터에 남은 완료 시각도 재참여 시 제거한다.
    tx.executeWithoutResult(status -> entityManager.createQuery("""
        update RoutineParticipation p set p.joinedAt = :previous, p.completedAt = :previous where p.id = :id
        """).setParameter("previous", LocalDateTime.of(2020, 1, 1, 0, 0))
        .setParameter("id", original.getId()).executeUpdate());
    mvc.perform(put(path).header(LoginUser.HEADER, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.inProgressCount").value(1));
    RoutineParticipation restarted = readParticipation(routineId, token);
    assertThat(restarted.getId()).isEqualTo(original.getId());
    assertThat(restarted.getJoinedAt()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
    assertThat(restarted.getCompletedAt()).isNull();
    assertThat(restarted.getDeletedAt()).isNull();

    mvc.perform(put(path + "/completion").header(LoginUser.HEADER, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.inProgressCount").value(0))
        .andExpect(jsonPath("$.result.completedCount").value(1))
        .andExpect(jsonPath("$.result.myParticipationStatus").value("COMPLETED"));
    LocalDateTime completedAt = readParticipation(routineId, token).getCompletedAt();
    assertThat(completedAt).isNotNull();
    mvc.perform(put(path + "/completion").header(LoginUser.HEADER, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.completedCount").value(1));
    assertThat(readParticipation(routineId, token).getCompletedAt()).isEqualTo(completedAt);
    mvc.perform(delete(path).header(LoginUser.HEADER, token))
        .andExpect(status().isConflict());
    mvc.perform(get(base + "/interactions").header(LoginUser.HEADER, token))
        .andExpect(jsonPath("$.result.completedCount").value(1))
        .andExpect(jsonPath("$.result.myParticipationStatus").value("COMPLETED"));
  }

  @Test
  void rejectsOwnerAndInvalidRequests() throws Exception {
    String path = "/api/routines/" + DemoData.ROUTINE_ID + "/participation";
    mvc.perform(put(path).header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PARTICIPATION403"));
    mvc.perform(delete(path).header(LoginUser.HEADER, DEMO_TOKEN)).andExpect(status().isForbidden());
    mvc.perform(put(path + "/completion").header(LoginUser.HEADER, DEMO_TOKEN)).andExpect(status().isForbidden());
    mvc.perform(put(path)).andExpect(status().isBadRequest());
    mvc.perform(put("/api/routines/invalid/participation").header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isBadRequest());
    mvc.perform(put("/api/routines/" + UNKNOWN_ID + "/participation").header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isNotFound());
  }

  @Test
  void swaggerCompletionExampleUsesANonOwnerWithActiveParticipation() throws Exception {
    mvc.perform(put("/api/routines/" + DemoData.ROUTINE_ID + "/participation/completion")
            .header(LoginUser.HEADER, DemoData.PARTICIPANT_DEVICE_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.myParticipationStatus").value("COMPLETED"));
  }

  private RoutineParticipation readParticipation(String routineId, String token) {
    return tx.execute(status -> entityManager.createQuery("""
        from RoutineParticipation p where p.routine.publicId = :routineId and p.user.deviceToken = :token
        """, RoutineParticipation.class).setParameter("routineId", routineId)
        .setParameter("token", token).getSingleResult());
  }
}
