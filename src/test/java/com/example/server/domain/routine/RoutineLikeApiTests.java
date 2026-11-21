package com.example.server.domain.routine;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.support.ApiTestSupport;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class RoutineLikeApiTests extends ApiTestSupport {

  @Test
  void repeatedPutAndDeleteKeepOneLikeAndReuseCanceledRow() throws Exception {
    String routineId = createRoutine(DEMO_USER_ID);
    String path = "/api/routines/" + routineId + "/like";
    String user = newUser();
    String otherUser = newUser();

    unlike(path, user).andExpect(jsonPath("$.result.likeCount").value(0));
    like(path, user).andExpect(jsonPath("$.result.likeCount").value(1))
        .andExpect(jsonPath("$.result.likedByMe").value(true));
    Long firstLikeId = likeId(routineId, user);
    like(path, user).andExpect(jsonPath("$.result.likeCount").value(1));

    like(path, otherUser).andExpect(jsonPath("$.result.likeCount").value(2));
    unlike(path, user).andExpect(jsonPath("$.result.likeCount").value(1))
        .andExpect(jsonPath("$.result.likedByMe").value(false));
    unlike(path, user).andExpect(jsonPath("$.result.likeCount").value(1));
    assertThat(likeId(routineId, user)).isEqualTo(firstLikeId);

    like(path, user).andExpect(jsonPath("$.result.likeCount").value(2))
        .andExpect(jsonPath("$.result.likedByMe").value(true));
    assertThat(likeId(routineId, user)).isEqualTo(firstLikeId);

    mvc.perform(get("/api/routines/" + routineId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.likeCount").value(2));
  }

  @Test
  void concurrentRequestsFromOneUserBothSucceedAndCountOnce() throws Exception {
    String path = "/api/routines/" + createRoutine(DEMO_USER_ID) + "/like";
    String token = newUser();
    CyclicBarrier start = new CyclicBarrier(2);
    Callable<Void> request = () -> {
      start.await(10, SECONDS);
      like(path, token).andExpect(jsonPath("$.result.likeCount").value(1));
      return null;
    };

    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(request);
      var second = executor.submit(request);
      first.get(20, SECONDS);
      second.get(20, SECONDS);
    }
  }

  @Test
  void validatesTokenAndRoutineIdAndRejectsMissingRoutines() throws Exception {
    mvc.perform(put("/api/routines/" + DemoData.ROUTINE_ID + "/like"))
        .andExpect(status().isUnauthorized());
    mvc.perform(put("/api/routines/invalid/like").header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isBadRequest());
    mvc.perform(delete("/api/routines/" + UNKNOWN_ID + "/like").header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROUTINE404"));
  }

  private ResultActions like(String path, String token) throws Exception {
    return mvc.perform(put(path).header(LoginUser.HEADER, authorization(token))).andExpect(status().isOk());
  }

  private ResultActions unlike(String path, String token) throws Exception {
    return mvc.perform(delete(path).header(LoginUser.HEADER, authorization(token))).andExpect(status().isOk());
  }

  private Long likeId(String routineId, String token) {
    return tx.execute(status -> entityManager.createQuery("""
        select l.id from RoutineLike l where l.routine.publicId = :routineId and l.user.publicId = :token
        """, Long.class).setParameter("routineId", routineId).setParameter("token", token).getSingleResult());
  }
}
