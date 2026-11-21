package com.example.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NewServerApplicationTests {

  @Autowired
  private MockMvc mvc;

  @Test
  void healthEndpointReturnsOk() throws Exception {
    mvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }

  @Test
  void successAndErrorResponsesUseTheCommonFormat() throws Exception {
    mvc.perform(get("/api/routines/01KB0000000000000000RTN001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.code").value("COMMON200"))
        .andExpect(jsonPath("$.message").isString())
        .andExpect(jsonPath("$.result").exists());

    mvc.perform(post("/api/routines")
            .header("X-Device-Token", "demo-device-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": ""}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400"))
        .andExpect(jsonPath("$.result.title").isString());

    mvc.perform(get("/api/routines/01KB0000000000000000ZZZ999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("ROUTINE404"));
  }
}
