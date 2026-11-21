package com.example.server.domain.routine;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class RoutineCommentApiTests extends ApiTestSupport {

  private static final String BODY = """
      {"content":"오늘부터 해볼게요."}
      """;

  @Test
  void createsListsEditsAndDeletesComments() throws Exception {
    String routineId = createRoutine(DEMO_TOKEN);
    String path = "/api/routines/" + routineId + "/comments";
    mvc.perform(get(path).header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").isEmpty());

    String first = createComment(path, DEMO_TOKEN, BODY)
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", startsWith(path + "/")))
        .andExpect(jsonPath("$.result.isMine").value(true))
        .andExpect(jsonPath("$.result.authorNickname").value("데모 유저"))
        .andExpect(jsonPath("$.result.profileImageUrl", endsWith("/api/images/" + DemoData.IMAGE_ID)))
        .andExpect(jsonPath("$.result.createdAt").isString())
        .andReturn().getResponse().getContentAsString();
    String firstId = JsonPath.read(first, "$.result.commentId");
    String createdAt = JsonPath.read(first, "$.result.createdAt");
    String otherToken = newUser();
    String second = createComment(path, otherToken, """
        {"content":"두 번째 댓글"}
        """).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    String secondId = JsonPath.read(second, "$.result.commentId");

    mvc.perform(get(path).header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result[0].commentId").value(firstId))
        .andExpect(jsonPath("$.result[0].isMine").value(true))
        .andExpect(jsonPath("$.result[1].commentId").value(secondId))
        .andExpect(jsonPath("$.result[1].isMine").value(false));

    String firstPath = path + "/" + firstId;
    mvc.perform(patch(firstPath).header(LoginUser.HEADER, DEMO_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"content":"내일부터 해볼게요."}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.content").value("내일부터 해볼게요."))
        .andExpect(jsonPath("$.result.createdAt").value(createdAt));

    mvc.perform(delete(firstPath).header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isOk());
    mvc.perform(get(path).header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(jsonPath("$.result.length()").value(1))
        .andExpect(jsonPath("$.result[0].commentId").value(secondId));
    mvc.perform(get("/api/routines/" + routineId))
        .andExpect(jsonPath("$.result.commentCount").value(1));
    mvc.perform(delete(firstPath).header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isNotFound());
  }

  @Test
  void verifiesCommentAuthorAndRoutine() throws Exception {
    String path = "/api/routines/" + DemoData.ROUTINE_ID + "/comments";
    String commentPath = path + "/" + DemoData.COMMENT_ID;
    String otherToken = newUser();

    mvc.perform(patch(commentPath).header(LoginUser.HEADER, otherToken)
            .contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("COMMENT403"));
    mvc.perform(delete(commentPath).header(LoginUser.HEADER, otherToken))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/routines/" + DemoData.EDITABLE_ROUTINE_ID + "/comments/" + DemoData.COMMENT_ID)
            .header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMENT404"));
    mvc.perform(get("/api/routines/" + UNKNOWN_ID + "/comments").header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROUTINE404"));
  }

  @Test
  void rejectsInvalidInputs() throws Exception {
    String path = "/api/routines/" + DemoData.ROUTINE_ID + "/comments";
    createComment(path, DEMO_TOKEN, """
        {"content":"  "}
        """).andExpect(status().isBadRequest());
    createComment(path, DEMO_TOKEN, "{\"content\":\"" + "가".repeat(51) + "\"}")
        .andExpect(status().isBadRequest());
    mvc.perform(get(path)).andExpect(status().isBadRequest());
    mvc.perform(delete(path + "/invalid").header(LoginUser.HEADER, DEMO_TOKEN))
        .andExpect(status().isBadRequest());
  }

  private ResultActions createComment(String path, String token, String body) throws Exception {
    return mvc.perform(post(path).header(LoginUser.HEADER, token)
        .contentType(MediaType.APPLICATION_JSON).content(body));
  }
}
