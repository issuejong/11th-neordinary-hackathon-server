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
}
