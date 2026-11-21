package com.example.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
  void exampleCanBeCreatedAndRetrieved() throws Exception {
    String location = mvc.perform(post("/api/examples")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "첫 번째 항목"}
                """))
        .andExpect(status().isCreated())
        .andExpect(header().exists(HttpHeaders.LOCATION))
        .andExpect(jsonPath("$.isSuccess").value(true))
        .andExpect(jsonPath("$.code").value("COMMON201"))
        .andExpect(jsonPath("$.result.id").isNumber())
        .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

    mvc.perform(get(location))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COMMON200"))
        .andExpect(jsonPath("$.result.name").value("첫 번째 항목"));
  }

  @Test
  void invalidAndMissingItemsUseTheCommonErrorFormat() throws Exception {
    mvc.perform(post("/api/examples")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": ""}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400"))
        .andExpect(jsonPath("$.result.name").isString());

    mvc.perform(get("/api/examples/0"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON404"));
  }
}
