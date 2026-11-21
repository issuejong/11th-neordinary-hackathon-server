package com.example.server.domain.routine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.global.auth.LoginUser;
import com.example.server.support.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ParticipationWorkApiTests extends ApiTestSupport {

  @Test
  void savesPrivateDraftAndPublishesEditableResult() throws Exception {
    String routineId = createRoutine(DEMO_USER_ID);
    String participant = newUser();
    String auth = authorization(participant);
    String owner = authorization(DEMO_USER_ID);
    String path = "/api/routines/" + routineId;
    String participation = path + "/participation";
    String results = path + "/participation-results";
    String result = results + "/" + participant;

    mvc.perform(put(participation).header(LoginUser.HEADER, auth)).andExpect(status().isOk());
    mvc.perform(put(participation + "/draft").header(LoginUser.HEADER, auth)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"cards":[null,{"title":"아침"}]}
                """))
        .andExpect(status().isOk());
    mvc.perform(get(participation + "/draft").header(LoginUser.HEADER, auth))
        .andExpect(jsonPath("$.result.cards[1].title").value("아침"));
    mvc.perform(get(participation + "/draft").header(LoginUser.HEADER, owner)).andExpect(status().isForbidden());
    mvc.perform(get(results)).andExpect(jsonPath("$.result.totalCount").value(0));
    mvc.perform(put(participation + "/completion").header(LoginUser.HEADER, auth)).andExpect(status().isBadRequest());

    mvc.perform(put(participation + "/completion").header(LoginUser.HEADER, auth)
            .contentType(MediaType.APPLICATION_JSON).content(participationResult(1)))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PARTICIPATION4002"));
    mvc.perform(put(participation + "/completion").header(LoginUser.HEADER, auth)
            .contentType(MediaType.APPLICATION_JSON).content(participationResult(2)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.result.completedCount").value(1));
    mvc.perform(get(results)).andExpect(jsonPath("$.result.totalCount").value(1));
    mvc.perform(get(result)).andExpect(jsonPath("$.result.work.title").value("함께한 나의 하루"));

    mvc.perform(put(result).header(LoginUser.HEADER, auth)
            .contentType(MediaType.APPLICATION_JSON).content(routineJson("수정한 나의 하루", null, cards(2))))
        .andExpect(jsonPath("$.result.work.title").value("수정한 나의 하루"));
    mvc.perform(delete(result).header(LoginUser.HEADER, owner)).andExpect(status().isForbidden());
    mvc.perform(delete(result).header(LoginUser.HEADER, auth)).andExpect(status().isOk());
    mvc.perform(get(result)).andExpect(status().isNotFound());
    mvc.perform(get(path + "/interactions").header(LoginUser.HEADER, auth))
        .andExpect(jsonPath("$.result.completedCount").value(0));
  }

  @Test
  void cancelClearsDraftBeforeRejoining() throws Exception {
    String routineId = createRoutine(DEMO_USER_ID);
    String auth = authorization(newUser());
    String path = "/api/routines/" + routineId + "/participation";
    mvc.perform(put(path).header(LoginUser.HEADER, auth)).andExpect(status().isOk());
    mvc.perform(put(path + "/draft").header(LoginUser.HEADER, auth)
        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"작성 중인 제목\"}")).andExpect(status().isOk());
    mvc.perform(delete(path).header(LoginUser.HEADER, auth)).andExpect(status().isOk());
    mvc.perform(put(path).header(LoginUser.HEADER, auth)).andExpect(status().isOk());
    mvc.perform(get(path + "/draft").header(LoginUser.HEADER, auth))
        .andExpect(jsonPath("$.result.cards").isEmpty());
  }

  @Test
  void publishesResultForLegacyStatusOnlyCompletionWithoutServerError() throws Exception {
    String participant = newUser();
    String auth = authorization(participant);
    String routineId = createRoutine(DEMO_USER_ID);
    String participation = "/api/routines/" + routineId + "/participation";

    mvc.perform(put(participation).header(LoginUser.HEADER, auth)).andExpect(status().isOk());
    // 이전 버전(빈 본문 완료)으로 만들어진 데이터: 상태만 COMPLETED, 작성 결과 없음
    Integer updated = tx.execute(status -> entityManager.createQuery("""
            update RoutineParticipation p
            set p.status = com.example.server.domain.routine.entity.RoutineParticipation.Status.COMPLETED,
                p.completedAt = CURRENT_TIMESTAMP
            where p.routine.id = (select r.id from Routine r where r.publicId = :r)
              and p.user.id = (select u.id from AppUser u where u.publicId = :u)
            """).setParameter("r", routineId).setParameter("u", participant).executeUpdate());
    org.assertj.core.api.Assertions.assertThat(updated).isEqualTo(1);
    Object stored = tx.execute(status -> entityManager.createQuery(
            "select p.status from RoutineParticipation p where p.routine.publicId = :r and p.user.publicId = :u")
        .setParameter("r", routineId).setParameter("u", participant).getSingleResult());
    org.assertj.core.api.Assertions.assertThat(stored.toString()).isEqualTo("COMPLETED");
    // 그 뒤 작성 결과를 제출해도 500이 아니라 결과가 공개되어야 한다
    mvc.perform(put(participation + "/completion").header(LoginUser.HEADER, auth)
            .contentType(MediaType.APPLICATION_JSON).content(participationResult(2)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/routines/" + routineId + "/participation-results"))
        .andExpect(jsonPath("$.result.totalCount").value(1));
  }
}
