package com.example.server.global.init;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.global.auth.LoginUser;
import com.example.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DemoDataRestoreTests extends ApiTestSupport {

  @Autowired
  private DemoDataInitializer initializer;

  @Test
  @DisplayName("Swagger 예시로 데모 루틴 · 댓글을 삭제해도 복구 작업이 되살린다")
  void restoresDeletedDemoData() throws Exception {
    mvc.perform(delete("/api/routines/" + DemoData.ROUTINE_ID + "/comments/" + DemoData.DELETABLE_COMMENT_ID)
            .header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/routines/" + DemoData.DELETABLE_ROUTINE_ID).header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/routines/" + DemoData.ROUTINE_ID + "/comments/" + DemoData.COMMENT_ID)
            .header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/routines/" + DemoData.EDITABLE_ROUTINE_ID).header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/routines/" + DemoData.EDITABLE_ROUTINE_ID)).andExpect(status().isNotFound());

    initializer.restoreDeletedDemoData();

    mvc.perform(get("/api/routines/" + DemoData.EDITABLE_ROUTINE_ID)).andExpect(status().isOk());
    mvc.perform(delete("/api/routines/" + DemoData.EDITABLE_ROUTINE_ID).header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isOk()); // 다시 삭제 예시 실행 가능
    initializer.restoreDeletedDemoData();
    mvc.perform(get("/api/routines/" + DemoData.ROUTINE_ID + "/comments").header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .jsonPath("$.result[?(@.commentId == '" + DemoData.COMMENT_ID + "')]").exists())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
            .jsonPath("$.result[?(@.commentId == '" + DemoData.DELETABLE_COMMENT_ID + "')]").exists());
    mvc.perform(get("/api/routines/" + DemoData.DELETABLE_ROUTINE_ID)).andExpect(status().isOk());
  }
}
