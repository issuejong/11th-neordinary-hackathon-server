package com.example.server.domain;

import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.global.util.Ulid;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiTests {

  private static final String NEW_USER = """
      {"nickname":"야행성"}
      """;

  @Autowired
  private MockMvc mvc;

  @Test
  void createsOneUserPerTokenAndReturnsTheSameProfileInBothLookups() throws Exception {
    String token = UUID.randomUUID().toString();
    mvc.perform(get("/api/users").header(LoginUser.HEADER, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").isEmpty());

    String json = mvc.perform(create(token, NEW_USER))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/users/me"))
        .andExpect(jsonPath("$.code").value("COMMON201"))
        .andExpect(jsonPath("$.result.publicId", matchesPattern(Ulid.REGEX)))
        .andExpect(jsonPath("$.result.nickname").value("야행성"))
        .andExpect(jsonPath("$.result.profileImagePublicId", in(List.of(
            DemoData.IMAGE_ID, DemoData.IMAGE_ID_2, DemoData.IMAGE_ID_3))))
        .andExpect(jsonPath("$.result.id").doesNotHaveJsonPath())
        .andExpect(jsonPath("$.result.deviceToken").doesNotHaveJsonPath())
        .andReturn().getResponse().getContentAsString();
    String userId = JsonPath.read(json, "$.result.publicId");
    String imageId = JsonPath.read(json, "$.result.profileImagePublicId");

    mvc.perform(create(token, """
            {"nickname":"두 번째 유저"}
            """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COMMON409"));

    mvc.perform(get("/api/users").header(LoginUser.HEADER, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.length()").value(1))
        .andExpect(jsonPath("$.result[0].publicId").value(userId))
        .andExpect(jsonPath("$.result[0].deviceToken").doesNotHaveJsonPath());

    mvc.perform(get("/api/users/me").header(LoginUser.HEADER, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.publicId").value(userId))
        .andExpect(jsonPath("$.result.nickname").value("야행성"))
        .andExpect(jsonPath("$.result.profileImagePublicId").value(imageId))
        .andExpect(jsonPath("$.result.deviceToken").doesNotHaveJsonPath());
  }

  @Test
  void rejectsInvalidDeviceHeadersAndNickname() throws Exception {
    mvc.perform(get("/api/users"))
        .andExpect(status().isBadRequest());
    mvc.perform(create(" ", NEW_USER))
        .andExpect(status().isBadRequest());
    mvc.perform(create("a".repeat(256), NEW_USER))
        .andExpect(status().isBadRequest());
    mvc.perform(create("valid-test-token", """
            {"nickname":"   "}
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"));
  }

  @Test
  void profileReturnsNotFoundForUnknownToken() throws Exception {
    mvc.perform(get("/api/users/me").header(LoginUser.HEADER, UUID.randomUUID().toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER404"));
  }

  private static MockHttpServletRequestBuilder create(String token, String body) {
    return post("/api/users").header(LoginUser.HEADER, token)
        .contentType(MediaType.APPLICATION_JSON).content(body);
  }
}
