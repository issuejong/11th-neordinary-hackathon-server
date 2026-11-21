package com.example.server.global.exception;

import com.example.server.support.TestSessions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({GlobalExceptionHandlerTests.TestControllerConfig.class, TestSessions.class})
class GlobalExceptionHandlerTests {

  @Autowired
  private TestSessions sessions;

  @Autowired
  private MockMvc mvc;

  @Test
  void invalidQueryParameterReturns400WithFieldName() throws Exception {
    mvc.perform(get("/test/errors/page").param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400"))
        .andExpect(jsonPath("$.result.size").isString());
  }

  @Test
  void missingOrMistypedQueryParameterReturns400() throws Exception {
    mvc.perform(get("/test/errors/page"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"));

    mvc.perform(get("/test/errors/page").param("size", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"));
  }

  @Test
  void malformedJsonReturns400() throws Exception {
    mvc.perform(post("/api/routines").header("Authorization", sessions.authorization("01KB0000000000000000MEM001"))
            .contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"));
  }

  @Test
  void missingFilePartReturns400() throws Exception {
    mvc.perform(multipart("/test/errors/upload"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"));
  }

  @Test
  void oversizedUploadReturnsFile400() throws Exception {
    mvc.perform(get("/test/errors/too-large"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("FILE400"));
  }

  @Test
  void unknownPathAndWrongMethodUseCommonFormat() throws Exception {
    mvc.perform(get("/no-such-path"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMON404"));

    mvc.perform(delete("/health"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("COMMON405"));
  }

  @Test
  void unsupportedAcceptHeaderReturns406WithoutErrorLog() throws Exception {
    mvc.perform(get("/api/routines").accept(MediaType.TEXT_PLAIN))
        .andExpect(status().isNotAcceptable());
    mvc.perform(get("/api/routines").accept(MediaType.APPLICATION_XML))
        .andExpect(status().isNotAcceptable());
  }

  @Test
  void errorResponsesAreJsonEvenWhenClientAcceptsOnlyOtherTypes() throws Exception {
    for (MediaType accept : new MediaType[] {MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML, MediaType.IMAGE_PNG, MediaType.TEXT_HTML}) {
      // 401/400/404 등 에러는 Accept와 무관하게 JSON (500으로 바뀌지 않음)
      mvc.perform(get("/api/users/me").accept(accept))
          .andExpect(status().is4xxClientError())
          .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
              .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.isSuccess").value(false));
      mvc.perform(get("/api/routines/abc").accept(accept))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("COMMON400"));
      mvc.perform(get("/api/routines/01KB0000000000000000ZZZ999").accept(accept))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ROUTINE404"));
    }
  }

  @Test
  void unexpectedExceptionReturns500WithoutInternalMessage() throws Exception {
    mvc.perform(get("/test/errors/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON500"))
        .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
  }

  @TestConfiguration
  static class TestControllerConfig {
    @RestController
    @RequestMapping("/test/errors")
    static class ErrorTestController {

      @GetMapping("/page")
      String page(@RequestParam @Min(1) @Max(50) int size) {
        return "ok";
      }

      @PostMapping("/upload")
      String upload(@RequestPart("file") MultipartFile file) {
        return "ok";
      }

      @GetMapping("/too-large")
      String tooLarge() {
        throw new MaxUploadSizeExceededException(10L * 1024 * 1024);
      }

      @GetMapping("/boom")
      String boom() {
        throw new IllegalStateException("secret internal detail");
      }
    }
  }
}
