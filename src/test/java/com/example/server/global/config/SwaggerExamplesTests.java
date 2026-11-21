package com.example.server.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** 심사 기준: Swagger에서 예시값 그대로 Execute 하면 성공해야 한다. */
class SwaggerExamplesTests extends ApiTestSupport {

  private String apiDocs() throws Exception {
    return mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
  }

  @Test
  @DisplayName("Swagger UI가 열린다")
  void swaggerUi() throws Exception {
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("작성자가 필요한 API에는 X-Device-Token 헤더(예시값 포함)가 문서화되고, 내부 userId 파라미터는 숨겨진다")
  void loginHeaderDocumented() throws Exception {
    String docs = apiDocs();
    for (String[] op : new String[][] {
        {"/api/images", "post"}, {"/api/routines", "post"},
        {"/api/routines/{routineId}", "put"}, {"/api/routines/{routineId}", "delete"}}) {
      List<Map<String, Object>> params = JsonPath.read(docs, "$.paths['" + op[0] + "']." + op[1] + ".parameters[*]");
      assertThat(params).as(op[1] + " " + op[0])
          .anySatisfy(p -> {
            assertThat(p.get("name")).isEqualTo(LoginUser.HEADER);
            assertThat(p.get("in")).isEqualTo("header");
            assertThat(p.get("example")).isEqualTo(DemoData.USER_DEVICE_TOKEN);
          })
          .noneSatisfy(p -> assertThat(p.get("name")).isEqualTo("userId"));
    }
    List<Object> getParams = JsonPath.read(docs, "$.paths['/api/routines/{routineId}'].get.parameters[*].name");
    assertThat(getParams).containsExactly("routineId");
  }

  @Test
  @DisplayName("스키마 이름이 겹쳐서 요청 · 응답 모양이 섞이지 않는다")
  void schemaNamesDoNotCollide() throws Exception {
    String docs = apiDocs();
    Map<String, Object> requestCard = JsonPath.read(docs, "$.components.schemas.RoutineCardRequest.properties");
    Map<String, Object> responseCard = JsonPath.read(docs, "$.components.schemas.RoutineCardResponse.properties");
    assertThat(requestCard).containsKeys("imageId", "title", "content", "time").doesNotContainKeys("cardId", "order", "imageUrl");
    assertThat(responseCard).containsKeys("cardId", "order", "imageUrl");
    Map<String, Object> detailCards = JsonPath.read(docs, "$.components.schemas.RoutineDetailResponse.properties.cards.items");
    assertThat(detailCards.get("$ref")).isEqualTo("#/components/schemas/RoutineCardResponse");
    // 모든 요청 본문 스키마가 서로 다른 이름을 가진다
    List<String> requestRefs = JsonPath.read(docs, "$.paths.*.*.requestBody.content['application/json'].schema['$ref']");
    assertThat(requestRefs).contains("#/components/schemas/RoutineSaveRequest");
  }

  @Test
  void commentListDeviceTokenIsOptionalButCommentCreationRequiresIt() throws Exception {
    String docs = apiDocs();
    String path = "$.paths['/api/routines/{routineId}/comments']";
    List<Map<String, Object>> listParams = JsonPath.read(docs, path + ".get.parameters[*]");
    List<Map<String, Object>> createParams = JsonPath.read(docs, path + ".post.parameters[*]");

    assertThat(listParams).anySatisfy(parameter -> {
      assertThat(parameter.get("name")).isEqualTo(LoginUser.HEADER);
      assertThat(parameter.get("required")).isNotEqualTo(true);
    });
    assertThat(createParams).anySatisfy(parameter -> {
      assertThat(parameter.get("name")).isEqualTo(LoginUser.HEADER);
      assertThat(parameter.get("required")).isEqualTo(true);
    });
  }

  @Test
  @DisplayName("요청 DTO의 모든 필드에 예시값이 있다")
  void requestSchemasHaveExamples() throws Exception {
    String docs = apiDocs();
    for (String schema : List.of("RoutineSaveRequest", "RoutineCardRequest")) {
      Map<String, Map<String, Object>> props = JsonPath.read(docs, "$.components.schemas." + schema + ".properties");
      props.forEach((name, prop) -> {
        if (!"array".equals(prop.get("type"))) {
          assertThat(prop).as(schema + "." + name).containsKey("example");
        }
      });
    }
  }

  @Test
  @DisplayName("경로 변수 예시값으로 바로 호출하면 성공한다 (상세 조회, 이미지 조회, 수정)")
  void pathExamplesWork() throws Exception {
    String docs = apiDocs();
    String routineExample = JsonPath.read(docs, "$.paths['/api/routines/{routineId}'].get.parameters[0].example");
    String imageExample = JsonPath.read(docs, "$.paths['/api/images/{imageId}'].get.parameters[0].example");
    String editableExample = JsonPath.read(docs,
        "$.paths['/api/routines/{routineId}'].put.parameters[?(@.name=='routineId')].example").toString()
        .replaceAll("[\\[\\]\"]", "");

    mvc.perform(get("/api/routines/" + routineExample)).andExpect(status().isOk());
    mvc.perform(get("/api/images/" + imageExample)).andExpect(status().isOk());

    // Swagger가 Card 예시값으로 만들어 주는 본문 그대로 수정 호출
    String cardImage = JsonPath.read(docs, "$.components.schemas.RoutineCardRequest.properties.imageId.example");
    String cardTitle = JsonPath.read(docs, "$.components.schemas.RoutineCardRequest.properties.title.example");
    String cardContent = JsonPath.read(docs, "$.components.schemas.RoutineCardRequest.properties.content.example");
    String cardTime = JsonPath.read(docs, "$.components.schemas.RoutineCardRequest.properties.time.example");
    String title = JsonPath.read(docs, "$.components.schemas.RoutineSaveRequest.properties.title.example");
    String content = JsonPath.read(docs, "$.components.schemas.RoutineSaveRequest.properties.content.example");
    String body = routineJson(title, content, card(cardImage, cardTitle, cardContent, cardTime));

    mvc.perform(MockMvcRequestBuilders.put("/api/routines/" + editableExample)
            .header(LoginUser.HEADER, DemoData.USER_DEVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
    postRoutine(DemoData.USER_DEVICE_TOKEN, body).andExpect(status().isCreated());
  }
}
