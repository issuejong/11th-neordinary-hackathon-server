package com.example.server.domain.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.domain.routine.entity.Routine;
import com.example.server.domain.routine.entity.RoutineLike;
import com.example.server.domain.routine.entity.RoutineParticipation;
import com.example.server.domain.user.entity.User;
import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("GET /api/routines 루틴 목록")
class RoutineListApiTests extends ApiTestSupport {

  private List<Map<String, Object>> list(String query) throws Exception {
    String json = mvc.perform(get("/api/routines" + query))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("COMMON200"))
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(json, "$.result.routines");
  }

  private static List<String> ids(List<Map<String, Object>> routines) {
    return routines.stream().map(r -> (String) r.get("routineId")).toList();
  }

  private Routine routine(String publicId) {
    return entityManager.createQuery("from Routine r where r.publicId = :p", Routine.class)
        .setParameter("p", publicId).getSingleResult();
  }

  private User user(String token) {
    return entityManager.createQuery("from AppUser u where u.publicId = :t", User.class)
        .setParameter("t", token).getSingleResult();
  }

  /** count명이 좋아요 (canceled명은 좋아요 후 취소) */
  private void like(String routineId, int count, int canceled) {
    List<String> tokens = java.util.stream.IntStream.range(0, count + canceled).mapToObj(i -> newUser()).toList();
    tx.executeWithoutResult(status -> {
      Routine routine = routine(routineId);
      for (int i = 0; i < tokens.size(); i++) {
        RoutineLike like = new RoutineLike(routine, user(tokens.get(i)));
        if (i >= count) {
          like.markDeleted(like.getUser().getId(), LocalDateTime.now());
        }
        entityManager.persist(like);
      }
    });
  }

  /** inProgress명 참여 중, completed명 완료, canceled명 참여 취소 */
  private void participate(String routineId, int inProgress, int completed, int canceled) {
    int total = inProgress + completed + canceled;
    List<String> tokens = java.util.stream.IntStream.range(0, total).mapToObj(i -> newUser()).toList();
    tx.executeWithoutResult(status -> {
      Routine routine = routine(routineId);
      for (int i = 0; i < total; i++) {
        RoutineParticipation p = new RoutineParticipation(routine, user(tokens.get(i)), LocalDateTime.now());
        if (i >= inProgress && i < inProgress + completed) {
          p.complete(LocalDateTime.now());
        } else if (i >= inProgress + completed) {
          p.markDeleted(p.getUser().getId(), LocalDateTime.now());
        }
        entityManager.persist(p);
      }
    });
  }

  private static long num(Map<String, Object> routine, String key) {
    return ((Number) routine.get(key)).longValue();
  }

  /** 정렬 기준 내림차순, 동점이면 최신순(publicId 내림차순)인지 전체 페이지에 대해 검증 */
  private static void assertSortedBy(List<Map<String, Object>> routines, String key) {
    for (int i = 1; i < routines.size(); i++) {
      Map<String, Object> prev = routines.get(i - 1);
      Map<String, Object> cur = routines.get(i);
      if (key != null) {
        assertThat(num(prev, key)).as("%s 내림차순 (%d번째)", key, i).isGreaterThanOrEqualTo(num(cur, key));
        if (num(prev, key) != num(cur, key)) {
          continue;
        }
      }
      assertThat((String) prev.get("routineId")).as("동점이면 최신순(ULID 내림차순) (%d번째)", i)
          .isGreaterThan((String) cur.get("routineId"));
    }
  }

  // ---------- 기본 / 최신순 ----------

  @Test
  @DisplayName("sort 생략 시 latest: 방금 만든 루틴이 맨 앞, 전체가 최신순")
  void defaultIsLatest() throws Exception {
    String older = createRoutine(DEMO_USER_ID);
    String newer = createRoutine(DEMO_USER_ID);

    List<Map<String, Object>> routines = list("?size=50");
    assertThat(ids(routines)).startsWith(newer, older);
    assertSortedBy(routines, null);
    assertThat(ids(list("?sort=latest&size=50"))).isEqualTo(ids(routines));
    assertThat(ids(list("?sort=&size=50"))).as("빈 값도 기본값(latest)").isEqualTo(ids(routines));
  }

  @Test
  @DisplayName("목록 항목: 작성자 · 썸네일(첫 스텝 이미지) · 스텝 수 · 좋아요 수 · 참여자 수")
  void itemFields() throws Exception {
    String routineId = createRoutine(DEMO_USER_ID);
    mvc.perform(get("/api/routines").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.routines[0].routineId").value(routineId))
        .andExpect(jsonPath("$.result.routines[0].title").value("테스트 루틴 제목"))
        .andExpect(jsonPath("$.result.routines[0].content").value("소개"))
        .andExpect(jsonPath("$.result.routines[0].owner.userId").value(DemoData.USER_ID))
        .andExpect(jsonPath("$.result.routines[0].owner.nickname").value("데모 유저"))
        .andExpect(jsonPath("$.result.routines[0].owner.profileImageUrl").value(startsWith("http")))
        .andExpect(jsonPath("$.result.routines[0].thumbnailUrl").value(endsWith("/api/images/" + DemoData.IMAGE_ID)))
        .andExpect(jsonPath("$.result.routines[0].cardCount").value(2))
        .andExpect(jsonPath("$.result.routines[0].likeCount").value(0))
        .andExpect(jsonPath("$.result.routines[0].participantCount").value(0))
        .andExpect(jsonPath("$.result.routines[0].createdAt").isString())
        .andExpect(jsonPath("$.result.routines[0].cards").doesNotExist());
  }

  @Test
  @DisplayName("삭제된 루틴은 목록에서 빠진다")
  void excludesDeleted() throws Exception {
    String routineId = createRoutine(DEMO_USER_ID);
    mvc.perform(delete("/api/routines/" + routineId).header(LoginUser.HEADER, authorization(DEMO_USER_ID)))
        .andExpect(status().isOk());
    for (String sort : List.of("latest", "likes", "participants")) {
      assertThat(ids(list("?size=50&sort=" + sort))).doesNotContain(routineId);
    }
  }

  @Test
  @DisplayName("인증 헤더 없이 조회할 수 있다")
  void publicAccess() throws Exception {
    mvc.perform(get("/api/routines")).andExpect(status().isOk());
  }

  // ---------- 좋아요순 ----------

  @Test
  @DisplayName("likes: 좋아요 많은 순, 취소된 좋아요는 세지 않음, 동점이면 최신순")
  void sortByLikes() throws Exception {
    String five = createRoutine(DEMO_USER_ID);
    String fourWithCanceled = createRoutine(DEMO_USER_ID);
    like(five, 5, 0);
    like(fourWithCanceled, 4, 3); // 취소 3건 → 4로 집계 (7이 아님)

    List<Map<String, Object>> routines = list("?sort=likes&size=50");
    assertThat(ids(routines)).startsWith(five, fourWithCanceled);
    assertThat(num(routines.get(0), "likeCount")).isEqualTo(5);
    assertThat(num(routines.get(1), "likeCount")).isEqualTo(4);
    assertSortedBy(routines, "likeCount");
  }

  // ---------- 참여자순 ----------

  @Test
  @DisplayName("participants: 참여 중 + 완료 인원순, 참여 취소는 세지 않음, 동점이면 최신순")
  void sortByParticipants() throws Exception {
    String six = createRoutine(DEMO_USER_ID);
    String fiveWithCanceled = createRoutine(DEMO_USER_ID);
    participate(six, 3, 3, 0);
    participate(fiveWithCanceled, 2, 3, 4); // 취소 4건 → 5로 집계

    List<Map<String, Object>> routines = list("?sort=participants&size=50");
    assertThat(ids(routines)).startsWith(six, fiveWithCanceled);
    assertThat(num(routines.get(0), "participantCount")).isEqualTo(6);
    assertThat(num(routines.get(1), "participantCount")).isEqualTo(5);
    assertSortedBy(routines, "participantCount");
  }

  @Test
  @DisplayName("sort 값은 대소문자를 구분하지 않는다")
  void caseInsensitiveSort() throws Exception {
    assertThat(ids(list("?sort=LIKES&size=50"))).isEqualTo(ids(list("?sort=likes&size=50")));
    assertThat(ids(list("?sort=Participants&size=50"))).isEqualTo(ids(list("?sort=participants&size=50")));
  }

  // ---------- 커서 페이지네이션 ----------

  private String page(String query) throws Exception {
    return mvc.perform(get("/api/routines" + query))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
  }

  /** nextCursor를 끝까지 따라가며 모든 routineId를 모은다 */
  private List<String> walk(String sort, int size) throws Exception {
    List<String> all = new java.util.ArrayList<>();
    String cursor = null;
    for (int guard = 0; guard < 1000; guard++) {
      String json = page("?sort=" + sort + "&size=" + size + (cursor == null ? "" : "&cursor=" + cursor));
      all.addAll(ids(JsonPath.read(json, "$.result.routines")));
      boolean hasNext = JsonPath.read(json, "$.result.hasNext");
      cursor = JsonPath.read(json, "$.result.nextCursor");
      assertThat(cursor != null).as("hasNext와 nextCursor 일치").isEqualTo(hasNext);
      if (!hasNext) {
        return all;
      }
    }
    throw new AssertionError("커서가 끝나지 않음");
  }

  @Test
  @DisplayName("첫 페이지: size · hasNext · nextCursor, 마지막 페이지는 nextCursor=null")
  void firstAndLastPage() throws Exception {
    createRoutine(DEMO_USER_ID);
    createRoutine(DEMO_USER_ID);
    mvc.perform(get("/api/routines").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.size").value(1))
        .andExpect(jsonPath("$.result.hasNext").value(true))
        .andExpect(jsonPath("$.result.nextCursor").isString())
        .andExpect(jsonPath("$.result.routines.length()").value(1))
        .andExpect(jsonPath("$.result.page").doesNotExist());
    mvc.perform(get("/api/routines").param("size", "50").param("sort", "latest"))
        .andExpect(status().isOk());
  }

  @ParameterizedTest(name = "sort={0}: 커서를 끝까지 따라가면 중복 · 누락 없이 한 번에 조회한 결과와 같다")
  @ValueSource(strings = {"latest", "likes", "participants"})
  void walkAllPages(String sort) throws Exception {
    for (int i = 0; i < 5; i++) {
      createRoutine(DEMO_USER_ID);
    }
    // 동점(0개)이 많은 상태 + 일부만 카운트가 있도록
    String top = createRoutine(DEMO_USER_ID);
    if (sort.equals("likes")) {
      like(top, 2, 0);
    } else if (sort.equals("participants")) {
      participate(top, 1, 1, 0);
    }

    List<String> walked = walk(sort, 3);
    assertThat(walked).doesNotHaveDuplicates();
    // 한 번에 조회 (size=50 페이지를 이어 붙임)
    assertThat(walked).isEqualTo(walk(sort, 50));
    assertThat(walked).contains(top, DemoData.ROUTINE_ID);
  }

  @Test
  @DisplayName("스크롤 중 새 루틴이 생겨도 다음 페이지에 중복이 생기지 않는다 (latest)")
  void noDuplicateWhenNewRoutineAdded() throws Exception {
    createRoutine(DEMO_USER_ID);
    createRoutine(DEMO_USER_ID);
    String first = page("?size=2");
    List<String> firstIds = ids(JsonPath.read(first, "$.result.routines"));
    String cursor = JsonPath.read(first, "$.result.nextCursor");

    String added = createRoutine(DEMO_USER_ID); // 첫 페이지를 본 뒤 새 글

    String second = page("?size=2&cursor=" + cursor);
    List<String> secondIds = ids(JsonPath.read(second, "$.result.routines"));
    assertThat(secondIds).doesNotContainAnyElementsOf(firstIds).doesNotContain(added);
  }

  @Test
  @DisplayName("커서 기준 루틴이 삭제돼도 다음 페이지를 계속 조회할 수 있다")
  void cursorSurvivesDeletion() throws Exception {
    String a = createRoutine(DEMO_USER_ID);
    String b = createRoutine(DEMO_USER_ID); // 최신순 첫 번째
    String first = page("?size=1");
    assertThat(ids(JsonPath.read(first, "$.result.routines"))).containsExactly(b);
    String cursor = JsonPath.read(first, "$.result.nextCursor");

    mvc.perform(delete("/api/routines/" + b).header(LoginUser.HEADER, authorization(DEMO_USER_ID))).andExpect(status().isOk());

    assertThat(ids(JsonPath.read(page("?size=1&cursor=" + cursor), "$.result.routines"))).containsExactly(a);
  }

  @Test
  @DisplayName("기본 size=20, 커서 없으면 첫 페이지")
  void defaults() throws Exception {
    mvc.perform(get("/api/routines"))
        .andExpect(jsonPath("$.result.size").value(20));
    assertThat(ids(list("?cursor="))).isEqualTo(ids(list("")));
  }

  // ---------- authorId (마이페이지 내 루틴) ----------

  private String publicIdOf(String token) {
    return tx.execute(status -> user(token).getPublicId());
  }

  @ParameterizedTest(name = "sort={0}&authorId=: 그 사용자의 루틴만, 정렬 · 응답 모양은 메인과 동일")
  @ValueSource(strings = {"latest", "likes", "participants"})
  void filterByAuthor(String sort) throws Exception {
    String me = newUser();
    String mine1 = createRoutine(me);
    String mine2 = createRoutine(me);
    String others = createRoutine(DEMO_USER_ID);
    like(mine1, 1, 0);
    participate(mine1, 1, 0, 0);

    List<Map<String, Object>> routines = list("?size=50&sort=" + sort + "&authorId=" + publicIdOf(me));
    assertThat(ids(routines)).containsExactlyInAnyOrder(mine1, mine2).doesNotContain(others);
    assertThat(routines).allSatisfy(r -> assertThat(((Map<?, ?>) r.get("owner")).get("userId")).isEqualTo(publicIdOf(me)));
    if (!sort.equals("latest")) {
      assertThat(ids(routines)).containsExactly(mine1, mine2); // 카운트 1 > 0
    } else {
      assertThat(ids(routines)).containsExactly(mine2, mine1); // 최신순
    }
    // 메인 목록 항목과 같은 필드
    assertThat(routines.getFirst().keySet()).isEqualTo(list("?size=1").getFirst().keySet());
  }

  @Test
  @DisplayName("authorId + 커서: 끝까지 따라가도 그 사용자 루틴만, 중복 없음")
  void filterByAuthorWithCursor() throws Exception {
    String me = newUser();
    List<String> mine = new java.util.ArrayList<>();
    for (int i = 0; i < 5; i++) {
      mine.add(createRoutine(me));
      createRoutine(DEMO_USER_ID); // 사이사이 다른 사람 루틴
    }
    String author = publicIdOf(me);
    List<String> walked = new java.util.ArrayList<>();
    String cursor = null;
    do {
      String json = page("?size=2&authorId=" + author + (cursor == null ? "" : "&cursor=" + cursor));
      walked.addAll(ids(JsonPath.read(json, "$.result.routines")));
      cursor = JsonPath.read(json, "$.result.nextCursor");
    } while (cursor != null);
    assertThat(walked).containsExactlyElementsOf(mine.reversed());
  }

  @Test
  @DisplayName("삭제한 내 루틴은 빠지고, 루틴이 없는 사용자 · 없는 사용자는 빈 목록")
  void filterByAuthorEdgeCases() throws Exception {
    String me = newUser();
    String deleted = createRoutine(me);
    mvc.perform(delete("/api/routines/" + deleted).header(LoginUser.HEADER, authorization(me))).andExpect(status().isOk());

    mvc.perform(get("/api/routines").param("authorId", publicIdOf(me)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.routines.length()").value(0))
        .andExpect(jsonPath("$.result.hasNext").value(false))
        .andExpect(jsonPath("$.result.nextCursor").doesNotExist());
    mvc.perform(get("/api/routines").param("authorId", UNKNOWN_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.routines.length()").value(0));
  }

  @ParameterizedTest(name = "authorId={0} → 400")
  @ValueSource(strings = {"abc", "demo-device-token", "01kb0000000000000000mem001", " "})
  void invalidAuthorId(String authorId) throws Exception {
    mvc.perform(get("/api/routines").param("authorId", authorId))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"))
        .andExpect(jsonPath("$.result.authorId").isString());
  }

  // ---------- participantId (프로필 참여 루틴 탭) · totalCount ----------

  private void joinVia(String userId, String routineId) throws Exception {
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .put("/api/routines/" + routineId + "/participation").header(LoginUser.HEADER, authorization(userId)))
        .andExpect(status().isOk());
  }

  private void completeVia(String userId, String routineId) throws Exception {
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .put("/api/routines/" + routineId + "/participation/completion").header(LoginUser.HEADER, authorization(userId)))
        .andExpect(status().isOk());
  }

  private void cancelVia(String userId, String routineId) throws Exception {
    mvc.perform(delete("/api/routines/" + routineId + "/participation").header(LoginUser.HEADER, authorization(userId)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("participantId: 참여 중 · 완료한 루틴만 최신순, 참여 취소 · 삭제된 루틴 · 남의 참여는 제외 (참여 API로 검증)")
  void participatedRoutines() throws Exception {
    String me = newUser();
    String inProgress = createRoutine(DEMO_USER_ID);
    String completed = createRoutine(DEMO_USER_ID);
    String canceled = createRoutine(DEMO_USER_ID);
    String deleted = createRoutine(DEMO_USER_ID);
    String othersOnly = createRoutine(DEMO_USER_ID);

    joinVia(me, inProgress);
    joinVia(me, completed);
    completeVia(me, completed);
    joinVia(me, canceled);
    cancelVia(me, canceled);
    joinVia(me, deleted);
    mvc.perform(delete("/api/routines/" + deleted).header(LoginUser.HEADER, authorization(DEMO_USER_ID))).andExpect(status().isOk());
    joinVia(newUser(), othersOnly);

    String json = page("?participantId=" + me);
    assertThat(ids(JsonPath.read(json, "$.result.routines"))).containsExactly(completed, inProgress); // 최신순
    assertThat(((Number) JsonPath.read(json, "$.result.totalCount")).longValue()).isEqualTo(2);
    // 카드 모양은 메인 목록과 같다
    assertThat(list("?size=1&participantId=" + me).getFirst().keySet())
        .isEqualTo(list("?size=1").getFirst().keySet());
  }

  @Test
  @DisplayName("participantId + 커서: 끝까지 따라가도 참여 루틴만, 중복 없음, totalCount는 페이지마다 같다")
  void participatedRoutinesWithCursor() throws Exception {
    String me = newUser();
    List<String> joined = new java.util.ArrayList<>();
    for (int i = 0; i < 5; i++) {
      String r = createRoutine(DEMO_USER_ID);
      joinVia(me, r);
      joined.add(r);
      createRoutine(DEMO_USER_ID); // 참여 안 한 루틴
    }
    List<String> walked = new java.util.ArrayList<>();
    String cursor = null;
    do {
      String json = page("?size=2&participantId=" + me + (cursor == null ? "" : "&cursor=" + cursor));
      walked.addAll(ids(JsonPath.read(json, "$.result.routines")));
      assertThat(((Number) JsonPath.read(json, "$.result.totalCount")).longValue()).isEqualTo(5);
      cursor = JsonPath.read(json, "$.result.nextCursor");
    } while (cursor != null);
    assertThat(walked).containsExactlyElementsOf(joined.reversed());
  }

  @Test
  @DisplayName("totalCount: 내 루틴 탭 개수 = 삭제 제외 내가 만든 루틴 수, 참여 없는 사용자는 0")
  void totalCountForProfileTabs() throws Exception {
    String me = newUser();
    createRoutine(me);
    createRoutine(me);
    String removed = createRoutine(me);
    mvc.perform(delete("/api/routines/" + removed).header(LoginUser.HEADER, authorization(me))).andExpect(status().isOk());

    mvc.perform(get("/api/routines").param("authorId", me).param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.totalCount").value(2))
        .andExpect(jsonPath("$.result.routines.length()").value(1))
        .andExpect(jsonPath("$.result.hasNext").value(true));
    mvc.perform(get("/api/routines").param("participantId", me))
        .andExpect(jsonPath("$.result.totalCount").value(0))
        .andExpect(jsonPath("$.result.routines.length()").value(0));
  }

  @Test
  @DisplayName("목록 항목에 댓글 수(commentCount)가 있다 (삭제된 댓글 제외)")
  void commentCountInList() throws Exception {
    String routineId = createRoutine(DEMO_USER_ID);
    for (String content : List.of("첫 댓글", "둘째 댓글")) {
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
              .post("/api/routines/" + routineId + "/comments").header(LoginUser.HEADER, authorization(DEMO_USER_ID))
              .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
              .content("{\"content\": \"" + content + "\"}"))
          .andExpect(status().isCreated());
    }
    mvc.perform(get("/api/routines").param("size", "1"))
        .andExpect(jsonPath("$.result.routines[0].routineId").value(routineId))
        .andExpect(jsonPath("$.result.routines[0].commentCount").value(2));
  }

  @ParameterizedTest(name = "participantId={0} → 400")
  @ValueSource(strings = {"abc", "demo-device-token", " "})
  void invalidParticipantId(String participantId) throws Exception {
    mvc.perform(get("/api/routines").param("participantId", participantId))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result.participantId").isString());
  }

  // ---------- 검증 ----------

  @ParameterizedTest(name = "sort={0} → 400")
  @ValueSource(strings = {"popular", "like", "latest,likes", " "})
  void invalidSort(String sort) throws Exception {
    mvc.perform(get("/api/routines").param("sort", sort))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400"))
        .andExpect(jsonPath("$.result.sort").isString());
  }

  @Test
  @DisplayName("size 0 / 51 / 숫자 아님 → 400 (어떤 파라미터인지 표시)")
  void invalidSize() throws Exception {
    mvc.perform(get("/api/routines").param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result.size").isString());
    mvc.perform(get("/api/routines").param("size", "51"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result.size").isString());
    mvc.perform(get("/api/routines").param("size", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.result.size").isString());
  }

  @ParameterizedTest(name = "잘못된 cursor \"{0}\" → 400 ROUTINE4001")
  @ValueSource(strings = {"abc", "!!!", "TEFURVNU", "bm90LWEtY3Vyc29y"})
  void invalidCursor(String cursor) throws Exception {
    mvc.perform(get("/api/routines").param("cursor", cursor))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ROUTINE4001"));
  }

  @Test
  @DisplayName("조작된 커서(음수 카운트, 잘못된 ULID) → 400 ROUTINE4001")
  void tamperedCursor() throws Exception {
    java.util.Base64.Encoder enc = java.util.Base64.getUrlEncoder().withoutPadding();
    for (String raw : List.of("LIKES|-1|01KB0000000000000000RTN001", "LIKES|1|not-a-ulid", "LIKES|x|01KB0000000000000000RTN001",
        "LIKES|1", "LATEST|0|01KB0000000000000000RTN001|extra")) {
      mvc.perform(get("/api/routines").param("sort", raw.startsWith("LATEST") ? "latest" : "likes")
              .param("cursor", enc.encodeToString(raw.getBytes())))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("ROUTINE4001"));
    }
  }

  @Test
  @DisplayName("다른 정렬에서 받은 커서 → 400 ROUTINE4001")
  void cursorFromOtherSort() throws Exception {
    createRoutine(DEMO_USER_ID);
    createRoutine(DEMO_USER_ID);
    String likesCursor = JsonPath.read(page("?sort=likes&size=1"), "$.result.nextCursor");
    mvc.perform(get("/api/routines").param("sort", "latest").param("cursor", likesCursor))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ROUTINE4001"));
    mvc.perform(get("/api/routines").param("sort", "likes").param("cursor", likesCursor))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("커서에 내부 PK가 들어가지 않는다 (publicId만)")
  void cursorHasNoInternalId() throws Exception {
    createRoutine(DEMO_USER_ID);
    createRoutine(DEMO_USER_ID);
    String json = page("?size=1");
    String cursor = JsonPath.read(json, "$.result.nextCursor");
    String lastId = JsonPath.read(json, "$.result.routines[0].routineId");
    String decoded = new String(java.util.Base64.getUrlDecoder().decode(cursor));
    assertThat(decoded).isEqualTo("LATEST|0|" + lastId);
  }

  @Test
  @DisplayName("size=50 (최대) → 200")
  void maxSize() throws Exception {
    mvc.perform(get("/api/routines").param("size", "50"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.size").value(50));
  }
}
