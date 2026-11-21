package com.example.server.domain;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.domain.image.entity.Image;
import com.example.server.domain.user.entity.User;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoutineImageApiTests {

  private static final String USER = DemoData.USER_DEVICE_TOKEN;
  private static final String UNKNOWN_ID = "01KB0000000000000000ZZZ999";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate tx;

  private static String body(String title, String cardTitle, String imageId) {
    return """
        {"title": "%s", "content": "소개", "cards": [
          {"imageId": "%s", "title": "%s", "content": "내용", "time": "07:30"},
          {"imageId": "%s", "title": "두 번째 카드 제목", "time": "08:00"}
        ]}
        """.formatted(title, imageId, cardTitle, DemoData.IMAGE_ID_2);
  }

  private String createRoutine() throws Exception {
    String json = mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("새로운 루틴", "첫 번째 카드 제목", DemoData.IMAGE_ID)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(json, "$.result.routineId");
  }

  // ---------- 이미지 ----------

  @Test
  void uploadAndFetchImage() throws Exception {
    byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
    String json = mvc.perform(multipart("/api/v1/images")
            .file(new MockMultipartFile("file", "a.png", "image/png", png))
            .header(LoginUser.HEADER, USER))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("COMMON201"))
        .andExpect(jsonPath("$.result.contentType").value("image/png"))
        .andExpect(jsonPath("$.result.byteSize").value(png.length))
        .andExpect(jsonPath("$.result.imageUrl").value(startsWith("http")))
        .andReturn().getResponse().getContentAsString();
    String imageId = JsonPath.read(json, "$.result.imageId");

    mvc.perform(get("/api/v1/images/" + imageId))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"))
        .andExpect(content().bytes(png));
  }

  @Test
  void seededDefaultImageIsServed() throws Exception {
    mvc.perform(get("/api/v1/images/" + DemoData.IMAGE_ID))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "image/png"));
  }

  @Test
  void imageUploadValidation() throws Exception {
    mvc.perform(multipart("/api/v1/images")
            .file(new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes()))
            .header(LoginUser.HEADER, USER))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("IMAGE4002"));

    mvc.perform(multipart("/api/v1/images")
            .file(new MockMultipartFile("file", "a.png", "image/png", new byte[0]))
            .header(LoginUser.HEADER, USER))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("IMAGE4001"));

    mvc.perform(multipart("/api/v1/images")
            .file(new MockMultipartFile("file", "a.png", "image/png", new byte[] {1})))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"));

    mvc.perform(multipart("/api/v1/images")
            .file(new MockMultipartFile("file", "a.png", "image/png", new byte[] {1}))
            .header(LoginUser.HEADER, "unknown-device"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER404"));
  }

  @Test
  void imageLookupErrors() throws Exception {
    mvc.perform(get("/api/v1/images/" + UNKNOWN_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("IMAGE404"));

    mvc.perform(get("/api/v1/images/not-a-ulid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result.imageId").isString());
  }

  // ---------- 루틴 ----------

  @Test
  void seededRoutineDetail() throws Exception {
    mvc.perform(get("/api/v1/routines/" + DemoData.ROUTINE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.title").value("갓생 아침 루틴"))
        .andExpect(jsonPath("$.result.owner.userId").value(DemoData.USER_ID))
        .andExpect(jsonPath("$.result.owner.profileImageUrl").value(startsWith("http")))
        .andExpect(jsonPath("$.result.cards.length()").value(3))
        .andExpect(jsonPath("$.result.cards[0].order").value(1))
        .andExpect(jsonPath("$.result.cards[0].time").value("07:00"))
        .andExpect(jsonPath("$.result.likeCount").value(0));
  }

  @Test
  void createUpdateDeleteRoutine() throws Exception {
    String routineId = createRoutine();

    mvc.perform(get("/api/v1/routines/" + routineId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.cards.length()").value(2))
        .andExpect(jsonPath("$.result.cards[0].time").value("07:30"))
        .andExpect(jsonPath("$.result.cards[1].order").value(2));

    // 카드 2장 → 1장으로 전체 교체
    mvc.perform(put("/api/v1/routines/" + routineId).header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": "수정된 루틴", "cards": [
                  {"imageId": "%s", "title": "바뀐 카드 제목입니다", "time": "09:00"}
                ]}
                """.formatted(DemoData.IMAGE_ID_3)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.title").value("수정된 루틴"))
        .andExpect(jsonPath("$.result.content").doesNotExist())
        .andExpect(jsonPath("$.result.cards.length()").value(1))
        .andExpect(jsonPath("$.result.cards[0].imageId").value(DemoData.IMAGE_ID_3));

    mvc.perform(delete("/api/v1/routines/" + routineId).header(LoginUser.HEADER, USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COMMON200"));

    mvc.perform(get("/api/v1/routines/" + routineId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROUTINE404"));
    mvc.perform(delete("/api/v1/routines/" + routineId).header(LoginUser.HEADER, USER))
        .andExpect(status().isNotFound());
  }

  @Test
  void routineValidationReturns400WithFieldErrors() throws Exception {
    // 카드 제목 5자 미만
    mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("루틴", "짧음", DemoData.IMAGE_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"))
        .andExpect(jsonPath("$.result.title").isString())
        .andExpect(jsonPath("$.result['cards[0].title']").isString());

    // 루틴 소개 501자
    mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": "저녁 루틴입니다", "content": "%s",
                 "cards": [{"imageId": "%s", "title": "카드 제목입니다", "time": "07:00"}]}
                """.formatted("가".repeat(501), DemoData.IMAGE_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result.content").isString());

    // 루틴 소개 500자는 통과
    mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": "저녁 루틴입니다", "content": "%s",
                 "cards": [{"imageId": "%s", "title": "카드 제목입니다", "time": "07:00"}]}
                """.formatted("가".repeat(500), DemoData.IMAGE_ID)))
        .andExpect(status().isCreated());

    // 시간 누락
    mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": "저녁 루틴입니다", "cards": [{"imageId": "%s", "title": "카드 제목입니다"}]}
                """.formatted(DemoData.IMAGE_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result['cards[0].time']").isString());

    // 제목 공백, 카드 없음
    mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": " ", "cards": []}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result.title").isString())
        .andExpect(jsonPath("$.result.cards").isString());

    // 시간 형식 오류
    mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": "저녁 루틴입니다", "cards": [{"imageId": "%s", "title": "카드 제목입니다", "time": "25:99"}]}
                """.formatted(DemoData.IMAGE_ID)))
        .andExpect(status().isBadRequest());

    // 카드 11장
    String card = "{\"imageId\": \"%s\", \"title\": \"카드 제목입니다\", \"time\": \"07:00\"}".formatted(DemoData.IMAGE_ID);
    mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\": \"저녁 루틴입니다\", \"cards\": [" + String.join(",", java.util.Collections.nCopies(11, card)) + "]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result.cards").isString());

    // 헤더 없음
    mvc.perform(post("/api/v1/routines")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("저녁 루틴입니다", "첫 번째 카드 제목", DemoData.IMAGE_ID)))
        .andExpect(status().isBadRequest());

    // 잘못된 ID 형식
    mvc.perform(get("/api/v1/routines/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result.routineId").isString());
  }

  @Test
  void routineNotFoundCases() throws Exception {
    mvc.perform(get("/api/v1/routines/" + UNKNOWN_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROUTINE404"));

    // 없는 이미지로 카드 생성
    mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, USER)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("저녁 루틴입니다", "첫 번째 카드 제목", UNKNOWN_ID)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("IMAGE404"));
  }

  @Test
  void onlyOwnerCanUpdateOrDelete() throws Exception {
    String routineId = createRoutine();
    String otherUser = "other-device";
    tx.executeWithoutResult(status -> {
      Image image = entityManager.createQuery("from Image i where i.publicId = :p", Image.class)
          .setParameter("p", DemoData.IMAGE_ID).getSingleResult();
      entityManager.persist(new User("01KB0000000000000000MEM002", otherUser, "다른 유저", image));
    });

    mvc.perform(put("/api/v1/routines/" + routineId).header(LoginUser.HEADER, otherUser)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("남의 루틴 수정", "첫 번째 카드 제목", DemoData.IMAGE_ID)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ROUTINE403"));

    mvc.perform(delete("/api/v1/routines/" + routineId).header(LoginUser.HEADER, otherUser))
        .andExpect(status().isForbidden());
  }

  @Test
  void cannotUseImageUploadedByAnotherUser() throws Exception {
    String json = mvc.perform(multipart("/api/v1/images")
            .file(new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2}))
            .header(LoginUser.HEADER, USER))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    String imageId = JsonPath.read(json, "$.result.imageId");
    tx.executeWithoutResult(status -> {
      Image image = entityManager.createQuery("from Image i where i.publicId = :p", Image.class)
          .setParameter("p", DemoData.IMAGE_ID).getSingleResult();
      entityManager.persist(new User("01KB0000000000000000MEM003", "third-device", "세 번째 유저", image));
    });

    mvc.perform(post("/api/v1/routines").header(LoginUser.HEADER, "third-device")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("저녁 루틴입니다", "첫 번째 카드 제목", imageId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("IMAGE404"));
  }
}
