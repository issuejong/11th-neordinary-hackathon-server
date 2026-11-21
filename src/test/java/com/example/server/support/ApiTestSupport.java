package com.example.server.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.domain.image.entity.Image;
import com.example.server.domain.user.entity.User;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.global.util.Ulid;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/** API 통합 테스트 공통 기반: H2(test 프로필) + 시드 데이터(DemoData) + 요청 헬퍼 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSessions.class)
public abstract class ApiTestSupport {

  protected static final String DEMO_USER_ID = DemoData.USER_ID;
  protected static final String UNKNOWN_ID = "01KB0000000000000000ZZZ999";

  @Autowired
  protected MockMvc mvc;

  @Autowired
  protected EntityManager entityManager;

  @Autowired
  protected TransactionTemplate tx;

  @Autowired
  private TestSessions sessions;

  protected String authorization(String publicId) {
    return sessions.authorization(publicId);
  }

  /** 기본 이미지를 프로필로 가진 새 사용자를 만들고 publicId를 반환한다. */
  protected String newUser() {
    String publicId = Ulid.generate();
    tx.executeWithoutResult(status -> {
      Image profile = entityManager.createQuery("from Image i where i.publicId = :p", Image.class)
          .setParameter("p", DemoData.IMAGE_ID).getSingleResult();
      entityManager.persist(new User(publicId, "테스트" + publicId, profile));
    });
    return publicId;
  }

  protected ResultActions uploadImage(String token, String contentType, byte[] data) throws Exception {
    return mvc.perform(multipart("/api/images")
        .file(new MockMultipartFile("file", "image", contentType, data))
        .header(LoginUser.HEADER, authorization(token)));
  }

  protected String uploadImageId(String token) throws Exception {
    String json = uploadImage(token, "image/png", TestImages.png())
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(json, "$.result.imageId");
  }

  protected ResultActions postRoutine(String token, String json) throws Exception {
    return mvc.perform(post("/api/routines").header(LoginUser.HEADER, authorization(token))
        .contentType(MediaType.APPLICATION_JSON).content(json));
  }

  protected String createRoutine(String token) throws Exception {
    String json = postRoutine(token, routineJson("테스트 루틴 제목", "소개", card(DemoData.IMAGE_ID, "첫 번째 스텝 제목", "07:00"),
        card(DemoData.IMAGE_ID_2, "두 번째 스텝 제목", "08:00")))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(json, "$.result.routineId");
  }

  // ---------- JSON 빌더 (null이면 필드 생략) ----------

  protected static String routineJson(String title, String content, String... cards) {
    List<String> fields = new ArrayList<>();
    if (title != null) {
      fields.add("\"title\": " + quote(title));
    }
    if (content != null) {
      fields.add("\"content\": " + quote(content));
    }
    fields.add("\"cards\": [" + String.join(",", cards) + "]");
    return "{" + String.join(",", fields) + "}";
  }

  protected static String card(String imageId, String title, String time) {
    return card(imageId, title, null, time);
  }

  protected static String card(String imageId, String title, String content, String time) {
    List<String> fields = new ArrayList<>();
    if (imageId != null) {
      fields.add("\"imageId\": " + quote(imageId));
    }
    if (title != null) {
      fields.add("\"title\": " + quote(title));
    }
    if (content != null) {
      fields.add("\"content\": " + quote(content));
    }
    if (time != null) {
      fields.add("\"time\": " + quote(time));
    }
    return "{" + String.join(",", fields) + "}";
  }

  protected static String validCard() {
    return card(DemoData.IMAGE_ID, "유효한 스텝 제목", "07:00");
  }

  protected static String participationResult(int stepCount) {
    return routineJson("함께한 나의 하루", null, cards(stepCount));
  }

  protected static String[] cards(int count) {
    String[] result = new String[count];
    for (int i = 0; i < count; i++) {
      result[i] = card(DemoData.IMAGE_ID, "스텝 제목 " + (i + 1) + "번", "07:00");
    }
    return result;
  }

  private static String quote(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
