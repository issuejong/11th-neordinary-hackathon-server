package com.example.server.domain.image;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.global.init.DemoData;
import com.example.server.support.ApiTestSupport;
import com.example.server.support.TestImages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** 이미지 정책은 설정(app.image.*)으로 바꿀 수 있다 */
@TestPropertySource(properties = {"app.image.max-dimension=100", "app.image.cache-max-age=1h"})
class ImagePropertiesTests extends ApiTestSupport {

  @Test
  @DisplayName("app.image.max-dimension 설정이 업로드 검증에 반영된다")
  void maxDimensionFromConfig() throws Exception {
    uploadImage(DEMO_TOKEN, "image/png", TestImages.png(100, 100)).andExpect(status().isCreated());
    uploadImage(DEMO_TOKEN, "image/png", TestImages.png(101, 10))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("IMAGE4003"));
  }

  @Test
  @DisplayName("app.image.cache-max-age 설정이 Cache-Control에 반영된다")
  void cacheMaxAgeFromConfig() throws Exception {
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/images/" + DemoData.IMAGE_ID))
        .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=3600")));
  }
}
