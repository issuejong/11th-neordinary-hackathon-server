package com.example.server.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.domain.user.repository.TestAccountLeaseRepository;
import com.example.server.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:test-accounts;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
class TestAccountApiTests extends ApiTestSupport {

  private static final String BASE = "/api/test-accounts";

  @Autowired
  private TestAccountLeaseRepository leases;

  @Test
  void anyoneCanRegisterUpToFiveSlotsWithoutCredentials() throws Exception {
    register(6, "테스트 여섯째").andExpect(status().isBadRequest());
    String id = createAccount(1, "테스트 첫째");
    register(1, "다른 이름").andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TEST4092"));
    register(4, "테스트 첫째").andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER4091"));
    mvc.perform(get(BASE)).andExpect(status().isOk())
        .andExpect(jsonPath("$.result[?(@.slot == 1)].publicId").value(id));
    mvc.perform(post("/api/users/connect")).andExpect(status().isNotFound());
  }

  @Test
  void claimRenewExpireAndRelease() throws Exception {
    String id = createAccount(2, "테스트 둘째");
    String claimPath = BASE + "/" + id + "/claim";
    String heartbeatPath = BASE + "/" + id + "/heartbeat";
    String bearer = claim(claimPath);
    mvc.perform(get(BASE)).andExpect(status().isOk())
        .andExpect(jsonPath("$.result[?(@.slot == 2)].available").value(false));
    mvc.perform(post(claimPath)).andExpect(status().isConflict());
    mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer))
        .andExpect(status().isOk()).andExpect(jsonPath("$.result.publicId").value(id))
        .andExpect(jsonPath("$.result.bio").value("소개"));
    mvc.perform(get("/api/users/me").header("X-User-Id", id)).andExpect(status().isUnauthorized());

    Instant earlierExpiry = Instant.now().plusSeconds(20);
    tx.executeWithoutResult(status -> leases.findByUser_PublicId(id).orElseThrow().renew(earlierExpiry));
    String renewed = mvc.perform(post(heartbeatPath).header(HttpHeaders.AUTHORIZATION, bearer))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    assertThat(Instant.parse(JsonPath.<String>read(renewed, "$.result.expiresAt"))).isAfter(earlierExpiry);

    tx.executeWithoutResult(status ->
        leases.findByUser_PublicId(id).orElseThrow().renew(Instant.now().minusSeconds(1)));
    mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isUnauthorized());
    mvc.perform(get(BASE)).andExpect(status().isOk())
        .andExpect(jsonPath("$.result[?(@.slot == 2)].available").value(true));
    String nextBearer = claim(claimPath);
    mvc.perform(delete(claimPath).header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isUnauthorized());
    mvc.perform(delete(claimPath).header(HttpHeaders.AUTHORIZATION, nextBearer)).andExpect(status().isOk());
    mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, nextBearer)).andExpect(status().isUnauthorized());
  }

  @Test
  void simultaneousClaimsHaveOneWinner() throws Exception {
    String id = createAccount(3, "테스트 셋째");
    String path = BASE + "/" + id + "/claim";
    var start = new CyclicBarrier(2);
    Callable<Integer> request = () -> {
      start.await(10, TimeUnit.SECONDS);
      return mvc.perform(post(path)).andReturn().getResponse().getStatus();
    };
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(request);
      var second = executor.submit(request);
      assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
    }
  }

  private ResultActions register(int slot, String nickname) throws Exception {
    return mvc.perform(post(BASE)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"slot\":%d,\"nickname\":\"%s\",\"bio\":\"소개\"}".formatted(slot, nickname)));
  }

  private String createAccount(int slot, String nickname) throws Exception {
    String response = register(slot, nickname)
        .andExpect(status().isCreated()).andExpect(jsonPath("$.result.profileImagePublicId").isString())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(response, "$.result.publicId");
  }

  private String claim(String path) throws Exception {
    String response = mvc.perform(post(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    return "Bearer " + JsonPath.<String>read(response, "$.result.accessToken");
  }
}
