package com.example.server.domain.routine;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineComment;
import com.example.server.domain.routine.entity.RoutineLike;
import com.example.server.domain.user.entity.User;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.global.util.Ulid;
import com.example.server.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RoutineApiTests extends ApiTestSupport {

  private static String ofLength(int length) {
    return "가".repeat(length);
  }

  // =====================================================================
  @Nested
  @DisplayName("POST /api/routines 루틴 생성")
  class Create {

    @Test
    @DisplayName("201 + Location 헤더 + 상세 응답, 스텝은 요청 순서대로 1..n")
    void created() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("갓생 저녁 루틴", "자기 전 루틴",
          card(DemoData.IMAGE_ID, "첫 번째 스텝 제목", "첫 내용", "21:00"),
          card(DemoData.IMAGE_ID_2, "두 번째 스텝 제목", "22:30")))
          .andExpect(status().isCreated())
          .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/routines/")))
          .andExpect(jsonPath("$.isSuccess").value(true))
          .andExpect(jsonPath("$.code").value("COMMON201"))
          .andExpect(jsonPath("$.result.routineId").value(org.hamcrest.Matchers.matchesPattern(Ulid.REGEX)))
          .andExpect(jsonPath("$.result.title").value("갓생 저녁 루틴"))
          .andExpect(jsonPath("$.result.content").value("자기 전 루틴"))
          .andExpect(jsonPath("$.result.owner.userId").value(DemoData.USER_ID))
          .andExpect(jsonPath("$.result.cards.length()").value(2))
          .andExpect(jsonPath("$.result.cards[0].order").value(1))
          .andExpect(jsonPath("$.result.cards[0].title").value("첫 번째 스텝 제목"))
          .andExpect(jsonPath("$.result.cards[0].content").value("첫 내용"))
          .andExpect(jsonPath("$.result.cards[0].time").value("21:00"))
          .andExpect(jsonPath("$.result.cards[0].imageId").value(DemoData.IMAGE_ID))
          .andExpect(jsonPath("$.result.cards[1].order").value(2))
          .andExpect(jsonPath("$.result.cards[1].time").value("22:30"))
          .andExpect(jsonPath("$.result.likeCount").value(0))
          .andExpect(jsonPath("$.result.commentCount").value(0));
    }

    @Test
    @DisplayName("응답에 기기 토큰과 내부 PK가 노출되지 않는다")
    void doesNotLeakSecrets() throws Exception {
      String body = postRoutine(DEMO_TOKEN, routineJson("노출 검사 루틴", null, validCard()))
          .andExpect(status().isCreated())
          .andReturn().getResponse().getContentAsString();
      org.assertj.core.api.Assertions.assertThat(body)
          .doesNotContain(DEMO_TOKEN)
          .doesNotContain("\"id\"")
          .doesNotContain("deviceToken");
    }

    @Test
    @DisplayName("소개는 선택이다 (생략 시 null)")
    void contentIsOptional() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("소개 없는 루틴", null, validCard()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.content").doesNotExist());
    }

    @Test
    @DisplayName("스텝 내용은 선택이다")
    void cardContentIsOptional() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("스텝 내용 없음", null, card(DemoData.IMAGE_ID, "내용 없는 스텝", "07:00")))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.cards[0].content").doesNotExist());
    }

    @Test
    @DisplayName("본인이 업로드한 이미지로 스텝을 만들 수 있다")
    void ownUploadedImage() throws Exception {
      String imageId = uploadImageId(DEMO_TOKEN);
      postRoutine(DEMO_TOKEN, routineJson("내 이미지 루틴", null, card(imageId, "내 이미지 스텝", "07:00")))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.cards[0].imageId").value(imageId))
          .andExpect(jsonPath("$.result.cards[0].imageUrl").value(org.hamcrest.Matchers.endsWith("/api/images/" + imageId)));
    }

    @Test
    @DisplayName("같은 이미지를 여러 스텝에 써도 된다")
    void sameImageForMultipleCards() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("같은 이미지 루틴", null, cards(3)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.cards.length()").value(3));
    }

    // ---- 루틴 제목 (필수, 5~30자) ----

    @ParameterizedTest(name = "제목 {0}자 → 201")
    @ValueSource(ints = {5, 30})
    void titleBoundaryOk(int length) throws Exception {
      postRoutine(DEMO_TOKEN, routineJson(ofLength(length), null, validCard()))
          .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "제목 {0}자 → 400")
    @ValueSource(ints = {0, 4, 31})
    void titleBoundaryFail(int length) throws Exception {
      postRoutine(DEMO_TOKEN, routineJson(ofLength(length), null, validCard()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.isSuccess").value(false))
          .andExpect(jsonPath("$.code").value("COMMON400"))
          .andExpect(jsonPath("$.result.title").isString());
    }

    @Test
    @DisplayName("제목 누락 / 공백만 → 400")
    void titleMissingOrBlank() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson(null, null, validCard()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.title").isString());
      postRoutine(DEMO_TOKEN, routineJson("       ", null, validCard()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.title").isString());
    }

    // ---- 루틴 소개 (선택, 최대 500자) ----

    @Test
    @DisplayName("소개 500자 → 201, 501자 → 400")
    void contentBoundary() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("소개 경계값 루틴", ofLength(500), validCard()))
          .andExpect(status().isCreated());
      postRoutine(DEMO_TOKEN, routineJson("소개 경계값 루틴", ofLength(501), validCard()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.content").isString());
    }

    // ---- 스텝 개수 (1~10) ----

    @Test
    @DisplayName("스텝 10개 → 201")
    void tenCards() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("스텝 열 개 루틴", null, cards(10)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.cards.length()").value(10))
          .andExpect(jsonPath("$.result.cards[9].order").value(10));
    }

    @Test
    @DisplayName("스텝 0개 / 11개 / 누락 → 400")
    void cardCountFail() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("스텝 없는 루틴", null))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.cards").isString());
      postRoutine(DEMO_TOKEN, routineJson("스텝 많은 루틴", null, cards(11)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.cards").isString());
      postRoutine(DEMO_TOKEN, "{\"title\": \"스텝 누락 루틴\"}")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.cards").isString());
      postRoutine(DEMO_TOKEN, "{\"title\": \"스텝 null 원소\", \"cards\": [null]}")
          .andExpect(status().isBadRequest());
    }

    // ---- 스텝 필드 ----

    @ParameterizedTest(name = "스텝 제목 {0}자 → 201")
    @ValueSource(ints = {5, 30})
    void cardTitleBoundaryOk(int length) throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("스텝 제목 경계", null, card(DemoData.IMAGE_ID, ofLength(length), "07:00")))
          .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "스텝 제목 {0}자 → 400")
    @ValueSource(ints = {0, 4, 31})
    void cardTitleBoundaryFail(int length) throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("스텝 제목 경계", null, card(DemoData.IMAGE_ID, ofLength(length), "07:00")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result['cards[0].title']").isString());
    }

    @Test
    @DisplayName("스텝 제목 누락 → 400")
    void cardTitleMissing() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("스텝 제목 누락", null, card(DemoData.IMAGE_ID, null, "07:00")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result['cards[0].title']").isString());
    }

    @Test
    @DisplayName("스텝 내용 100자 → 201, 101자 → 400")
    void cardContentBoundary() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("스텝 내용 경계", null,
          card(DemoData.IMAGE_ID, "스텝 제목입니다", ofLength(100), "07:00")))
          .andExpect(status().isCreated());
      postRoutine(DEMO_TOKEN, routineJson("스텝 내용 경계", null,
          card(DemoData.IMAGE_ID, "스텝 제목입니다", ofLength(101), "07:00")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result['cards[0].content']").isString());
    }

    @Test
    @DisplayName("스텝 시간 누락 → 400 (필수)")
    void cardTimeMissing() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("시간 누락 루틴", null, card(DemoData.IMAGE_ID, "스텝 제목입니다", null)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result['cards[0].time']").isString());
    }

    @ParameterizedTest(name = "스텝 시간 \"{0}\" → 400")
    @ValueSource(strings = {"25:00", "07:60", "7:00", "07:00:00", "아침", ""})
    void cardTimeInvalidFormat(String time) throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("시간 형식 루틴", null, card(DemoData.IMAGE_ID, "스텝 제목입니다", time)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("COMMON400"));
    }

    @ParameterizedTest(name = "스텝 시간 \"{0}\" → 201")
    @ValueSource(strings = {"00:00", "23:59"})
    void cardTimeBoundaryOk(String time) throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("시간 경계 루틴", null, card(DemoData.IMAGE_ID, "스텝 제목입니다", time)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.result.cards[0].time").value(time));
    }

    @Test
    @DisplayName("스텝 이미지 누락 / 형식 오류 → 400")
    void cardImageMissingOrInvalid() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("이미지 누락 루틴", null, card(null, "스텝 제목입니다", "07:00")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result['cards[0].imageId']").isString());
      postRoutine(DEMO_TOKEN, routineJson("이미지 형식 루틴", null, card("not-a-ulid", "스텝 제목입니다", "07:00")))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result['cards[0].imageId']").isString());
    }

    @Test
    @DisplayName("없는 이미지 → 404 IMAGE404")
    void cardImageNotFound() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("없는 이미지 루틴", null, card(UNKNOWN_ID, "스텝 제목입니다", "07:00")))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("IMAGE404"));
    }

    @Test
    @DisplayName("다른 사용자가 올린 이미지 → 404 IMAGE404")
    void otherUsersImage() throws Exception {
      String othersImage = uploadImageId(newUser());
      postRoutine(DEMO_TOKEN, routineJson("남의 이미지 루틴", null, card(othersImage, "스텝 제목입니다", "07:00")))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("IMAGE404"));
    }

    @Test
    @DisplayName("여러 필드가 틀리면 모두 result에 담긴다")
    void multipleFieldErrors() throws Exception {
      postRoutine(DEMO_TOKEN, routineJson("짧음", ofLength(501), card(null, "짧음", null)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.title").isString())
          .andExpect(jsonPath("$.result.content").isString())
          .andExpect(jsonPath("$.result['cards[0].imageId']").isString())
          .andExpect(jsonPath("$.result['cards[0].title']").isString())
          .andExpect(jsonPath("$.result['cards[0].time']").isString());
    }

    // ---- 요청 형식 / 인증 ----

    @Test
    @DisplayName("깨진 JSON / 빈 본문 → 400, JSON이 아닌 Content-Type → 415")
    void malformedRequest() throws Exception {
      postRoutine(DEMO_TOKEN, "{\"title\": ")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("COMMON400"));
      postRoutine(DEMO_TOKEN, "")
          .andExpect(status().isBadRequest());
      postRoutine(DEMO_TOKEN, "{\"title\": \"타입 오류 루틴\", \"cards\": \"문자열\"}")
          .andExpect(status().isBadRequest());
      mvc.perform(post("/api/routines").header(LoginUser.HEADER, DEMO_TOKEN)
              .contentType(MediaType.TEXT_PLAIN).content("hello"))
          .andExpect(status().isUnsupportedMediaType())
          .andExpect(jsonPath("$.code").value("COMMON415"));
    }

    @Test
    @DisplayName("X-Device-Token 누락 → 400, 모르는 토큰 → 404 USER404, 255자 초과 → 400")
    void authHeader() throws Exception {
      mvc.perform(post("/api/routines").contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("헤더 없는 루틴", null, validCard())))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("COMMON400"));
      postRoutine("unknown-device-token", routineJson("모르는 토큰 루틴", null, validCard()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("USER404"));
      postRoutine("x".repeat(256), routineJson("긴 토큰 루틴", null, validCard()))
          .andExpect(status().isBadRequest());
    }
  }

  // =====================================================================
  @Nested
  @DisplayName("GET /api/routines/{routineId} 루틴 상세")
  class Detail {

    @Test
    @DisplayName("시드 루틴: 작성자, 스텝(순서대로), 이미지 URL, 카운트")
    void seededRoutine() throws Exception {
      mvc.perform(get("/api/routines/" + DemoData.ROUTINE_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("COMMON200"))
          .andExpect(jsonPath("$.result.routineId").value(DemoData.ROUTINE_ID))
          .andExpect(jsonPath("$.result.owner.userId").value(DemoData.USER_ID))
          .andExpect(jsonPath("$.result.owner.nickname").value("데모 유저"))
          .andExpect(jsonPath("$.result.owner.profileImageUrl").value(startsWith("http")))
          .andExpect(jsonPath("$.result.cards.length()").value(3))
          .andExpect(jsonPath("$.result.cards[0].order").value(1))
          .andExpect(jsonPath("$.result.cards[1].order").value(2))
          .andExpect(jsonPath("$.result.cards[2].order").value(3))
          .andExpect(jsonPath("$.result.cards[0].time").value("07:00"))
          .andExpect(jsonPath("$.result.cards[0].imageUrl").value(startsWith("http")))
          .andExpect(jsonPath("$.result.createdAt").isString())
          .andExpect(jsonPath("$.result.updatedAt").isString());
    }

    @Test
    @DisplayName("인증 헤더 없이 조회 가능")
    void publicAccess() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      mvc.perform(get("/api/routines/" + routineId))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("좋아요 · 댓글 수는 삭제(취소)되지 않은 것만 센다")
    void counts() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      tx.executeWithoutResult(status -> {
        Routine routine = entityManager.createQuery("from Routine r where r.publicId = :p", Routine.class)
            .setParameter("p", routineId).getSingleResult();
        List<User> users = entityManager.createQuery("from AppUser u", User.class).setMaxResults(1).getResultList();
        User liker = users.getFirst();
        entityManager.persist(new RoutineLike(routine, liker));
        RoutineLike canceled = new RoutineLike(routine, entityManager.createQuery(
            "from AppUser u where u.deviceToken = :t", User.class)
            .setParameter("t", newUserInline()).getSingleResult());
        canceled.markDeleted(canceled.getUser().getId(), LocalDateTime.now());
        entityManager.persist(canceled);
        entityManager.persist(new RoutineComment(Ulid.generate(), routine, liker, "좋은 루틴이에요"));
        RoutineComment deleted = new RoutineComment(Ulid.generate(), routine, liker, "삭제된 댓글");
        deleted.markDeleted(liker.getId(), LocalDateTime.now());
        entityManager.persist(deleted);
      });

      mvc.perform(get("/api/routines/" + routineId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.likeCount").value(1))
          .andExpect(jsonPath("$.result.commentCount").value(1));
    }

    @Test
    @DisplayName("없는 루틴 → 404 ROUTINE404, 잘못된 ID 형식 → 400")
    void notFoundAndInvalid() throws Exception {
      mvc.perform(get("/api/routines/" + UNKNOWN_ID))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.isSuccess").value(false))
          .andExpect(jsonPath("$.code").value("ROUTINE404"));
      mvc.perform(get("/api/routines/abc"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.routineId").isString());
      mvc.perform(get("/api/routines/" + UNKNOWN_ID.toLowerCase()))
          .andExpect(status().isBadRequest());
    }
  }

  // =====================================================================
  @Nested
  @DisplayName("PUT /api/routines/{routineId} 루틴 수정")
  class Update {

    @Test
    @DisplayName("제목 · 소개 수정, 스텝은 전체 교체 (개수 · 순서 · ID 변경)")
    void replacesCards() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      String before = mvc.perform(get("/api/routines/" + routineId))
          .andReturn().getResponse().getContentAsString();
      String oldCardId = JsonPath.read(before, "$.result.cards[0].cardId");

      mvc.perform(put("/api/routines/" + routineId).header(LoginUser.HEADER, DEMO_TOKEN)
              .contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("수정된 루틴 제목", "수정된 소개",
                  card(DemoData.IMAGE_ID_3, "바뀐 첫 번째 스텝", "06:00"),
                  card(DemoData.IMAGE_ID, "바뀐 두 번째 스텝", "06:30"),
                  card(DemoData.IMAGE_ID_2, "새로 추가된 스텝", "07:00"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("COMMON200"))
          .andExpect(jsonPath("$.result.title").value("수정된 루틴 제목"))
          .andExpect(jsonPath("$.result.content").value("수정된 소개"))
          .andExpect(jsonPath("$.result.cards.length()").value(3))
          .andExpect(jsonPath("$.result.cards[0].title").value("바뀐 첫 번째 스텝"))
          .andExpect(jsonPath("$.result.cards[0].imageId").value(DemoData.IMAGE_ID_3))
          .andExpect(jsonPath("$.result.cards[2].order").value(3))
          .andExpect(jsonPath("$.result.cards[0].cardId").value(not(oldCardId)));

      // 재조회해도 같은 결과 (DB 반영 확인)
      mvc.perform(get("/api/routines/" + routineId))
          .andExpect(jsonPath("$.result.title").value("수정된 루틴 제목"))
          .andExpect(jsonPath("$.result.cards.length()").value(3));
    }

    @Test
    @DisplayName("스텝을 줄이고, 소개를 생략하면 소개가 지워진다")
    void shrinkAndClearContent() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      mvc.perform(put("/api/routines/" + routineId).header(LoginUser.HEADER, DEMO_TOKEN)
              .contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("스텝 하나만 남김", null, validCard())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.content").doesNotExist())
          .andExpect(jsonPath("$.result.cards.length()").value(1));
    }

    @Test
    @DisplayName("순서만 바꿔도 유니크 키 충돌 없이 저장된다")
    void reorder() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      mvc.perform(put("/api/routines/" + routineId).header(LoginUser.HEADER, DEMO_TOKEN)
              .contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("순서 바꾼 루틴", null,
                  card(DemoData.IMAGE_ID_2, "두 번째 스텝 제목", "08:00"),
                  card(DemoData.IMAGE_ID, "첫 번째 스텝 제목", "07:00"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.result.cards[0].title").value("두 번째 스텝 제목"))
          .andExpect(jsonPath("$.result.cards[1].title").value("첫 번째 스텝 제목"));
    }

    @Test
    @DisplayName("생성과 같은 검증 규칙 → 400")
    void validation() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      mvc.perform(put("/api/routines/" + routineId).header(LoginUser.HEADER, DEMO_TOKEN)
              .contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("짧음", null, cards(11))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.result.title").isString())
          .andExpect(jsonPath("$.result.cards").isString());

      // 검증 실패 시 원래 데이터는 그대로
      mvc.perform(get("/api/routines/" + routineId))
          .andExpect(jsonPath("$.result.title").value("테스트 루틴 제목"))
          .andExpect(jsonPath("$.result.cards.length()").value(2));
    }

    @Test
    @DisplayName("없는 이미지로 수정 → 404, 기존 스텝은 롤백되어 유지")
    void rollbackOnMissingImage() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      mvc.perform(put("/api/routines/" + routineId).header(LoginUser.HEADER, DEMO_TOKEN)
              .contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("없는 이미지로 수정", null, card(UNKNOWN_ID, "스텝 제목입니다", "07:00"))))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("IMAGE404"));

      mvc.perform(get("/api/routines/" + routineId))
          .andExpect(jsonPath("$.result.title").value("테스트 루틴 제목"))
          .andExpect(jsonPath("$.result.cards.length()").value(2));
    }

    @Test
    @DisplayName("작성자가 아니면 403 ROUTINE403, 데이터는 그대로")
    void forbidden() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      mvc.perform(put("/api/routines/" + routineId).header(LoginUser.HEADER, newUser())
              .contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("남의 루틴 수정", null, validCard())))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ROUTINE403"));
      mvc.perform(get("/api/routines/" + routineId))
          .andExpect(jsonPath("$.result.title").value("테스트 루틴 제목"));
    }

    @Test
    @DisplayName("없는 루틴 → 404, 헤더 누락 → 400")
    void notFoundAndNoHeader() throws Exception {
      mvc.perform(put("/api/routines/" + UNKNOWN_ID).header(LoginUser.HEADER, DEMO_TOKEN)
              .contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("없는 루틴 수정", null, validCard())))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ROUTINE404"));
      mvc.perform(put("/api/routines/" + UNKNOWN_ID)
              .contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("헤더 없는 수정", null, validCard())))
          .andExpect(status().isBadRequest());
    }
  }

  // =====================================================================
  @Nested
  @DisplayName("DELETE /api/routines/{routineId} 루틴 삭제")
  class Delete {

    @Test
    @DisplayName("삭제 → 200, 이후 조회 · 수정 · 재삭제는 404")
    void softDelete() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      mvc.perform(delete("/api/routines/" + routineId).header(LoginUser.HEADER, DEMO_TOKEN))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.isSuccess").value(true))
          .andExpect(jsonPath("$.code").value("COMMON200"))
          .andExpect(jsonPath("$.result").doesNotExist());

      mvc.perform(get("/api/routines/" + routineId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ROUTINE404"));
      mvc.perform(put("/api/routines/" + routineId).header(LoginUser.HEADER, DEMO_TOKEN)
              .contentType(MediaType.APPLICATION_JSON)
              .content(routineJson("삭제된 루틴 수정", null, validCard())))
          .andExpect(status().isNotFound());
      mvc.perform(delete("/api/routines/" + routineId).header(LoginUser.HEADER, DEMO_TOKEN))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("소프트 삭제: DB에는 deleted_at · deleted_by가 기록된다")
    void recordsDeletion() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      mvc.perform(delete("/api/routines/" + routineId).header(LoginUser.HEADER, DEMO_TOKEN))
          .andExpect(status().isOk());
      Routine routine = tx.execute(status -> entityManager
          .createQuery("from Routine r where r.publicId = :p", Routine.class)
          .setParameter("p", routineId).getSingleResult());
      org.assertj.core.api.Assertions.assertThat(routine.getDeletedAt()).isNotNull();
      org.assertj.core.api.Assertions.assertThat(routine.getDeletedBy()).isNotNull();
    }

    @Test
    @DisplayName("작성자가 아니면 403, 루틴은 남아 있다")
    void forbidden() throws Exception {
      String routineId = createRoutine(DEMO_TOKEN);
      mvc.perform(delete("/api/routines/" + routineId).header(LoginUser.HEADER, newUser()))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ROUTINE403"));
      mvc.perform(get("/api/routines/" + routineId))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("없는 루틴 → 404, 잘못된 ID → 400, 헤더 누락 → 400")
    void errors() throws Exception {
      mvc.perform(delete("/api/routines/" + UNKNOWN_ID).header(LoginUser.HEADER, DEMO_TOKEN))
          .andExpect(status().isNotFound());
      mvc.perform(delete("/api/routines/abc").header(LoginUser.HEADER, DEMO_TOKEN))
          .andExpect(status().isBadRequest());
      mvc.perform(delete("/api/routines/" + UNKNOWN_ID))
          .andExpect(status().isBadRequest());
    }
  }

  /** tx 안에서 쓰기 위한 사용자 생성 (newUser는 자체 트랜잭션을 연다) */
  private String newUserInline() {
    String token = "device-inline-" + java.util.UUID.randomUUID();
    entityManager.persist(new User(Ulid.generate(), token, "인라인 유저",
        entityManager.createQuery("from Image i where i.publicId = :p", com.example.server.domain.image.entity.Image.class)
            .setParameter("p", DemoData.IMAGE_ID).getSingleResult()));
    return token;
  }
}
