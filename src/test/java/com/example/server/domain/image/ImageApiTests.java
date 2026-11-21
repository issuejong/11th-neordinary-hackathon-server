package com.example.server.domain.image;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.domain.image.entity.Image;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.global.util.Ulid;
import com.example.server.support.ApiTestSupport;
import com.example.server.support.TestImages;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

class ImageApiTests extends ApiTestSupport {

  @Nested
  @DisplayName("POST /api/images 이미지 업로드")
  class Upload {

    @Test
    @DisplayName("201 + Location + imageId · contentType · byteSize · width · height · imageUrl, DB에 바이너리 저장")
    void uploaded() throws Exception {
      byte[] png = TestImages.png(40, 30);
      String json = uploadImage(DEMO_TOKEN, "image/png", png)
          .andExpect(status().isCreated())
          .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/images/")))
          .andExpect(jsonPath("$.isSuccess").value(true))
          .andExpect(jsonPath("$.code").value("COMMON201"))
          .andExpect(jsonPath("$.result.imageId").value(matchesPattern(Ulid.REGEX)))
          .andExpect(jsonPath("$.result.contentType").value("image/png"))
          .andExpect(jsonPath("$.result.byteSize").value(png.length))
          .andExpect(jsonPath("$.result.width").value(40))
          .andExpect(jsonPath("$.result.height").value(30))
          .andExpect(jsonPath("$.result.imageUrl").value(startsWith("http")))
          .andReturn().getResponse().getContentAsString();
      String imageId = JsonPath.read(json, "$.result.imageId");
      org.hamcrest.MatcherAssert.assertThat(JsonPath.read(json, "$.result.imageUrl"), endsWith("/api/images/" + imageId));

      Image saved = tx.execute(status -> entityManager
          .createQuery("from Image i where i.publicId = :p", Image.class)
          .setParameter("p", imageId).getSingleResult());
      org.assertj.core.api.Assertions.assertThat(saved.getData()).isEqualTo(png);
      org.assertj.core.api.Assertions.assertThat(saved.getByteSize()).isEqualTo(png.length);
      org.assertj.core.api.Assertions.assertThat(saved.getCreatedBy()).isNotNull();
    }

    // ---- 형식: 파일 내용으로 판별 ----

    @Test
    @DisplayName("jpg · png · gif · webp · heic · heif 모두 업로드되고, 실제 형식과 해상도가 응답된다")
    void allowedFormats() throws Exception {
      expectUploaded(TestImages.encode("jpg", 30, 20), "image/jpeg", 30, 20);
      expectUploaded(TestImages.png(30, 20), "image/png", 30, 20);
      expectUploaded(TestImages.encode("gif", 30, 20), "image/gif", 30, 20);
      expectUploaded(TestImages.webp(1920, 1080), "image/webp", 1920, 1080);
      expectUploaded(TestImages.heic("heic", 4032, 3024), "image/heic", 4032, 3024);
      expectUploaded(TestImages.heic("mif1", 4032, 3024), "image/heif", 4032, 3024);
    }

    private void expectUploaded(byte[] data, String type, int width, int height) throws Exception {
      // 클라이언트가 보낸 Content-Type과 무관하게 파일 내용으로 판별
      uploadImage(DEMO_TOKEN, "application/octet-stream", data)
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.contentType").value(type))
          .andExpect(jsonPath("$.result.width").value(width))
          .andExpect(jsonPath("$.result.height").value(height));
    }

    @Test
    @DisplayName("Content-Type을 잘못 보내도(jpg를 image/png로) 실제 형식으로 저장된다")
    void wrongDeclaredType() throws Exception {
      uploadImage(DEMO_TOKEN, "image/png", TestImages.encode("jpg", 10, 10))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.contentType").value("image/jpeg"));
    }

    @Test
    @DisplayName("Content-Type 없이 보내도 이미지면 업로드된다")
    void noContentType() throws Exception {
      mvc.perform(multipart("/api/images")
              .file(new MockMultipartFile("file", "image", null, TestImages.png()))
              .header(LoginUser.HEADER, DEMO_TOKEN))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.contentType").value("image/png"));
    }

    @ParameterizedTest(name = "이미지로 위장한 {0} → 400 IMAGE4002")
    @ValueSource(strings = {"텍스트 파일입니다", "%PDF-1.7 fake", "<svg xmlns=\"http://www.w3.org/2000/svg\"/>",
        "<html><script>alert(1)</script></html>", "BM fake bitmap"})
    void disguisedFiles(String content) throws Exception {
      uploadImage(DEMO_TOKEN, "image/png", content.getBytes(StandardCharsets.UTF_8))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.isSuccess").value(false))
          .andExpect(jsonPath("$.code").value("IMAGE4002"));
    }

    @Test
    @DisplayName("시그니처만 맞고 헤더가 손상된 파일 → 400 IMAGE4002")
    void corruptedHeader() throws Exception {
      byte[] truncatedPng = java.util.Arrays.copyOf(TestImages.png(), 12);
      uploadImage(DEMO_TOKEN, "image/png", truncatedPng)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("IMAGE4002"));
      byte[] jpegMagicOnly = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0};
      uploadImage(DEMO_TOKEN, "image/jpeg", jpegMagicOnly)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("IMAGE4002"));
      uploadImage(DEMO_TOKEN, "image/heic", TestImages.heic("avis", 100, 100))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("IMAGE4002"));
    }

    // ---- 해상도: 가로 · 세로 최대 8192px ----

    @Test
    @DisplayName("아이폰 48MP 원본(8064x6048), 8192x8192 → 201")
    void dimensionOk() throws Exception {
      uploadImage(DEMO_TOKEN, "image/heic", TestImages.heic("heic", 8064, 6048))
          .andExpect(status().isCreated());
      uploadImage(DEMO_TOKEN, "image/png", TestImages.pngClaiming(8192, 8192))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.width").value(8192));
    }

    @Test
    @DisplayName("가로 또는 세로가 8192px 초과 → 400 IMAGE4003")
    void dimensionTooLarge() throws Exception {
      uploadImage(DEMO_TOKEN, "image/png", TestImages.pngClaiming(8193, 100))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("IMAGE4003"));
      uploadImage(DEMO_TOKEN, "image/png", TestImages.pngClaiming(100, 8193))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("IMAGE4003"));
      uploadImage(DEMO_TOKEN, "image/webp", TestImages.webp(16000, 4000))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("IMAGE4003"));
      uploadImage(DEMO_TOKEN, "image/heic", TestImages.heic("heic", 9000, 9000))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("IMAGE4003"));
    }

    // ---- 요청 형식 / 인증 ----

    @Test
    @DisplayName("빈 파일 → 400 IMAGE4001")
    void emptyFile() throws Exception {
      uploadImage(DEMO_TOKEN, "image/png", new byte[0])
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("IMAGE4001"));
    }

    @Test
    @DisplayName("file 파트 누락 / 다른 이름의 파트 → 400")
    void missingPart() throws Exception {
      mvc.perform(multipart("/api/images").header(LoginUser.HEADER, DEMO_TOKEN))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("COMMON400"));
      mvc.perform(multipart("/api/images")
              .file(new MockMultipartFile("image", "a.png", "image/png", TestImages.png()))
              .header(LoginUser.HEADER, DEMO_TOKEN))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("multipart가 아닌 요청 → 415")
    void notMultipart() throws Exception {
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/images")
              .header(LoginUser.HEADER, DEMO_TOKEN)
              .contentType("application/json").content("{}"))
          .andExpect(status().isUnsupportedMediaType())
          .andExpect(jsonPath("$.code").value("COMMON415"));
    }

    @Test
    @DisplayName("헤더 누락 → 400, 모르는 기기 토큰 → 404 USER404")
    void authHeader() throws Exception {
      mvc.perform(multipart("/api/images").file(new MockMultipartFile("file", "a.png", "image/png", TestImages.png())))
          .andExpect(status().isBadRequest());
      uploadImage("unknown-device-token", "image/png", TestImages.png())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("USER404"));
    }
  }

  @Nested
  @DisplayName("GET /api/images/{imageId} 이미지 조회")
  class Find {

    @Test
    @DisplayName("업로드한 바이트 그대로 + 실제 Content-Type · 캐시 · nosniff 헤더, 인증 불필요")
    void returnsBinary() throws Exception {
      byte[] webp = TestImages.webp(64, 64);
      String imageId = JsonPath.read(uploadImage(DEMO_TOKEN, "image/webp", webp)
          .andReturn().getResponse().getContentAsString(), "$.result.imageId");

      mvc.perform(get("/api/images/" + imageId))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/webp"))
          .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=")))
          .andExpect(header().string("X-Content-Type-Options", "nosniff"))
          .andExpect(content().bytes(webp));
    }

    @Test
    @DisplayName("시드 기본 이미지는 실제 256x256 PNG 파일이다")
    void seededDefaultImage() throws Exception {
      byte[] body = mvc.perform(get("/api/images/" + DemoData.IMAGE_ID))
          .andExpect(status().isOk())
          .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
          .andReturn().getResponse().getContentAsByteArray();
      org.assertj.core.api.Assertions.assertThat(
          javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(body)).getWidth()).isEqualTo(256);
    }

    @Test
    @DisplayName("없는 이미지 → 404 IMAGE404 (JSON), 잘못된 ID → 400")
    void errors() throws Exception {
      mvc.perform(get("/api/images/" + UNKNOWN_ID))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.isSuccess").value(false))
          .andExpect(jsonPath("$.code").value("IMAGE404"));
      mvc.perform(get("/api/images/not-a-ulid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.imageId").isString());
    }
  }
}
