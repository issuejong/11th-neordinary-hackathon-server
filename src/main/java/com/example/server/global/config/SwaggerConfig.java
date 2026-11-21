package com.example.server.global.config;

import com.example.server.global.auth.LoginUser;
import com.example.server.global.init.DemoData;
import com.example.server.global.response.code.status.ErrorStatus;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class SwaggerConfig {

  private static final String ERROR_SCHEMA = "ErrorResponse";

  static {
    // @LoginUser Long 파라미터는 쿼리 파라미터로 문서화하지 않는다 (헤더로 대체)
    SpringDocUtils.getConfig().addAnnotationsToIgnore(LoginUser.class);
  }

  @Bean
  public OpenAPI serverOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("NEORDINARY HACKATHON 8조 — 루틴 공유 API")
            .version("v1")
            .description(guide()))
        .tags(List.of(
            new Tag().name("User").description("기기 토큰 기반 가입 · 내 프로필"),
            new Tag().name("Image").description("이미지 업로드(DB 저장) · 조회"),
            new Tag().name("Routine").description("루틴 목록 · 생성 · 상세 · 수정 · 삭제"),
            new Tag().name("Routine Comment").description("루틴 댓글"),
            new Tag().name("Health").description("서버 상태 확인")))
        .components(new Components().addSchemas(ERROR_SCHEMA, errorSchema()));
  }

  /** 문서 상단 안내: 프론트가 처음 읽을 사용법 + 에러 코드 전체 목록(ErrorStatus에서 자동 생성) */
  private static String guide() {
    String errorRows = Arrays.stream(ErrorStatus.values())
        .map(e -> "| " + e.getHttpStatus().value() + " | `" + e.getCode() + "` | " + e.getMessage() + " |")
        .collect(Collectors.joining("\n"));
    return """
        ### 공통
        - 모든 JSON 응답: `{ "isSuccess": boolean, "code": string, "message": string, "result": ... }`
          - 성공: `isSuccess=true`, `code=COMMON200`(조회 · 수정 · 삭제) / `COMMON201`(생성)
          - 실패: `isSuccess=false`, `code`로 분기하세요. 입력값 오류(400)는 `result`에 **필드별 오류 메시지**가 담깁니다.
            예) `"result": { "title": "루틴 제목은 5자 이상 30자 이하여야 합니다.", "cards[0].time": "시간은 필수입니다." }`
        - ID는 모두 **publicId(ULID 26자, 예: `01KB0000000000000000RTN001`)** 입니다.
        - 시각(`createdAt` 등)은 **KST(Asia/Seoul)** 기준 `yyyy-MM-dd'T'HH:mm:ss.SSSSSS` (시간대 표기 없음), 스텝 시간은 `HH:mm`.

        ### 인증 (임시)
        - 로그인 사용자가 필요한 API는 헤더 **`X-Device-Token: <기기 토큰>`** 을 보냅니다.
        - 처음엔 `POST /api/users`로 가입하고, 이후 같은 토큰으로 호출합니다. 모르는 토큰이면 `404 USER404`.
        - Swagger 예시값은 데모 사용자 토큰(`%s`)이라 그대로 Execute 하면 됩니다.

        ### 루틴 만들기 흐름
        1. `POST /api/images` (multipart `file`) → `result.imageId`
        2. `POST /api/routines` 의 `cards[].imageId` 에 넣어 생성 (스텝 1~10개, 배열 순서 = 스텝 순서)
        3. 화면에는 응답의 `imageUrl` / `thumbnailUrl` 을 그대로 쓰면 됩니다.

        ### 목록 (커서 페이지네이션)
        - `GET /api/routines?sort=latest|likes|participants&size=20` → `result.nextCursor`
        - 다음 페이지: 같은 `sort`로 `&cursor=<nextCursor>`. `hasNext=false`면 끝 (`nextCursor=null`)
        - 마이페이지 내 루틴: `&authorId=<내 publicId>` 추가 (응답 모양 동일)

        ### 데모 데이터
        Swagger 예시용 루틴 · 댓글은 삭제 예시를 실행해도 **1분 안에 자동 복구**됩니다.

        ### 에러 코드 전체
        | HTTP | code | message |
        | --- | --- | --- |
        %s
        """.formatted(DemoData.USER_DEVICE_TOKEN, errorRows);
  }

  private static Schema<?> errorSchema() {
    return new ObjectSchema()
        .description("실패 응답 (모든 에러 공통)")
        .addProperty("isSuccess", new BooleanSchema().example(false))
        .addProperty("code", new StringSchema().description("에러 코드 (아래 표 참고)").example("COMMON400"))
        .addProperty("message", new StringSchema().description("사용자에게 보여줄 수 있는 메시지").example("요청 형식이 올바르지 않습니다."))
        .addProperty("result", new ObjectSchema()
            .description("400 입력값 오류일 때만: { 필드명: 오류 메시지 }")
            .additionalProperties(new StringSchema())
            .example(Map.of("title", "루틴 제목은 5자 이상 30자 이하여야 합니다.")));
  }

  /**
   * 모든 API 공통 문서 보강
   * - @LoginUser가 있으면 X-Device-Token 헤더(예시값 포함) 추가
   * - 생성(POST + ResponseEntity) 응답은 201로 표시
   * - 발생 가능한 에러 응답(400 / 403 / 404 / 500)과 예시 추가
   */
  @Bean
  public OperationCustomizer commonOperationCustomizer() {
    return (operation, handlerMethod) -> {
      boolean needsLogin = Arrays.stream(handlerMethod.getMethodParameters())
          .anyMatch(p -> p.hasParameterAnnotation(LoginUser.class));
      if (needsLogin) {
        operation.addParametersItem(new HeaderParameter()
            .name(LoginUser.HEADER)
            .required(true)
            .description("요청한 사용자의 기기 토큰 (임시 인증). 데모 사용자: " + DemoData.USER_DEVICE_TOKEN)
            .example(DemoData.USER_DEVICE_TOKEN)
            .schema(new StringSchema()));
      }
      markCreated(operation, handlerMethod);
      if (!handlerMethod.getMethod().getDeclaringClass().getPackageName().endsWith(".health")) {
        addErrorResponses(operation, handlerMethod, needsLogin);
      }
      return operation;
    };
  }

  private static void markCreated(Operation operation, HandlerMethod handlerMethod) {
    ApiResponses responses = operation.getResponses();
    if (handlerMethod.hasMethodAnnotation(PostMapping.class)
        && ResponseEntity.class.isAssignableFrom(handlerMethod.getMethod().getReturnType())
        && responses != null && responses.containsKey("200")) {
      ApiResponse ok = responses.remove("200");
      responses.addApiResponse("201", ok.description("생성 성공 (COMMON201)"));
    }
  }

  private static void addErrorResponses(Operation operation, HandlerMethod handlerMethod, boolean needsLogin) {
    boolean hasPathVariable = Arrays.stream(handlerMethod.getMethodParameters())
        .anyMatch(p -> p.hasParameterAnnotation(org.springframework.web.bind.annotation.PathVariable.class));
    boolean mutatesOwned = needsLogin && hasPathVariable
        && (handlerMethod.hasMethodAnnotation(org.springframework.web.bind.annotation.PutMapping.class)
        || handlerMethod.hasMethodAnnotation(org.springframework.web.bind.annotation.PatchMapping.class)
        || handlerMethod.hasMethodAnnotation(org.springframework.web.bind.annotation.DeleteMapping.class));
    String domain = handlerMethod.getBeanType().getSimpleName();
    String notFoundCode = domain.contains("Comment") ? "COMMENT404 · ROUTINE404"
        : domain.contains("Routine") ? "ROUTINE404" : domain.contains("Image") ? "IMAGE404" : "USER404";

    Map<String, ApiResponse> errors = new LinkedHashMap<>();
    errors.put("400", error("입력값 오류 (COMMON400 등) — result에 필드별 메시지"
        + (needsLogin ? " / X-Device-Token 헤더 누락" : ""), "COMMON400", "요청 형식이 올바르지 않습니다.",
        Map.of("필드명", "오류 메시지")));
    if (mutatesOwned) {
      errors.put("403", domain.contains("Comment")
          ? error("댓글 작성자가 아님", "COMMENT403", "댓글 작성자만 수정하거나 삭제할 수 있습니다.", null)
          : error("루틴 작성자가 아님", "ROUTINE403", "루틴 작성자만 수정하거나 삭제할 수 있습니다.", null));
    }
    if (domain.contains("User") && handlerMethod.hasMethodAnnotation(PostMapping.class)) {
      errors.put("409", error("이미 가입된 기기 토큰", "COMMON409", "이미 처리된 요청이거나 데이터가 충돌합니다.", null));
    }
    if (needsLogin || hasPathVariable) {
      errors.put("404", error("대상 없음 (" + notFoundCode + (needsLogin ? " · 모르는 토큰 USER404" : "") + ")",
          notFoundCode.split(" ")[0], "리소스를 찾을 수 없습니다.", null));
    }
    errors.put("500", error("서버 내부 오류", "COMMON500", "서버 내부 오류가 발생했습니다.", null));

    if (operation.getResponses() == null) {
      operation.setResponses(new ApiResponses());
    }
    errors.forEach((code, response) -> operation.getResponses().putIfAbsent(code, response));
  }

  private static ApiResponse error(String description, String code, String message, Object result) {
    Map<String, Object> example = new LinkedHashMap<>();
    example.put("isSuccess", false);
    example.put("code", code);
    example.put("message", message);
    if (result != null) {
      example.put("result", result);
    }
    return new ApiResponse()
        .description(description)
        .content(new Content().addMediaType("application/json", new MediaType()
            .schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA))
            .example(example)));
  }
}
