package com.example.server.global.config;

import com.example.server.global.auth.LoginUser;
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
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  public static final String TEST_ACCOUNT_SESSION = "TestAccountSession";

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
            new Tag().name("Test Account").description("① 심사 시작: 8번 데모 유저 claim → Authorize"),
            new Tag().name("User").description("프로필 조회 · 수정"),
            new Tag().name("Image").description("이미지 업로드(DB 저장) · 조회"),
            new Tag().name("Routine").description("루틴 목록 · 생성 · 상세 · 수정 · 삭제"),
            new Tag().name("Routine Like").description("좋아요 설정 · 취소"),
            new Tag().name("Routine Comment").description("댓글 목록 · 작성 · 수정 · 삭제"),
            new Tag().name("Routine Participation").description("함께하기 참여 · 초안 · 완료"),
            new Tag().name("Routine Participation Results").description("함께한 결과 목록 · 상세 · 수정 · 삭제"),
            new Tag().name("Health").description("서버 상태 확인")))
        .components(new Components().addSchemas(ERROR_SCHEMA, errorSchema())
            .addSecuritySchemes(TEST_ACCOUNT_SESSION, new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .scheme("bearer").description("계정 선택 응답의 accessToken을 입력합니다.")));
  }

  /** 문서 상단 안내: 프론트가 처음 읽을 사용법 + 에러 코드 전체 목록(ErrorStatus에서 자동 생성) */
  private static String guide() {
    String errorRows = Arrays.stream(ErrorStatus.values())
        .map(e -> "<tr><td>" + e.getHttpStatus().value() + "</td><td><code>" + e.getCode() + "</code></td><td>"
            + e.getMessage() + "</td></tr>")
        .collect(Collectors.joining());
    return """
        > **심사용 빠른 시작**
        > 1. **Test Account** › `POST /claim` 을 예시값(8번 · 데모 유저) 그대로 실행
        > 2. 응답의 `accessToken` 을 오른쪽 위 **Authorize** 에 붙여넣기
        > 3. 이후 모든 API를 **예시값 그대로** 실행 — 예시 데이터는 10초마다 자동 복구됩니다

        ### 응답 형식
        모든 응답은 `isSuccess` · `code` · `message` · `result` 로 구성됩니다.
        성공은 `COMMON200`(조회 · 수정 · 삭제) / `COMMON201`(생성), 실패는 `code` 로 구분합니다.
        입력값 오류(400)는 `result` 에 필드별 메시지가 담깁니다. 예: `"cards[0].time": "시간은 필수입니다."`

        ### 규칙
        - **ID** — 외부에 노출되는 ID는 모두 publicId (ULID 26자)
        - **시각** — KST 기준 `yyyy-MM-dd'T'HH:mm:ss`, 스텝 시간은 `HH:mm`
        - **인증** — `Authorization: Bearer <accessToken>` (테스트 계정 claim으로 발급)

        ### 주요 흐름
        - **루틴 만들기** — 이미지 업로드 → 받은 `imageId` 를 `cards[].imageId` 에 넣어 생성 (스텝 1~10개)
        - **목록** — `sort=latest | likes | participants`, 다음 페이지는 응답의 `nextCursor` 를 `cursor` 로 전달
        - **프로필** — 내 루틴 `authorId=<publicId>`, 참여 루틴 `participantId=<publicId>`

        <details><summary><b>에러 코드 전체 보기</b></summary>
        <table><thead><tr><th>HTTP</th><th>code</th><th>message</th></tr></thead><tbody>%s</tbody></table>
        </details>
        """.formatted(errorRows);
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
   * - @LoginUser가 있으면 테스트 계정 세션을 문서화
   * - 생성(POST + ResponseEntity) 응답은 201로 표시
   * - 발생 가능한 에러 응답(400 / 403 / 404 / 500)과 예시 추가
   */
  @Bean
  public OperationCustomizer commonOperationCustomizer() {
    return (operation, handlerMethod) -> {
      LoginUser loginUser = Arrays.stream(handlerMethod.getMethodParameters())
          .map(parameter -> parameter.getParameterAnnotation(LoginUser.class))
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
      if (loginUser != null) {
        if (!loginUser.required()) {
          operation.addSecurityItem(new SecurityRequirement());
        }
        operation.addSecurityItem(new SecurityRequirement().addList(TEST_ACCOUNT_SESSION));
        addLoginGuide(operation, loginUser.required());
      }
      markCreated(operation, handlerMethod);
      if (!handlerMethod.getMethod().getDeclaringClass().getPackageName().endsWith(".health")) {
        addErrorResponses(operation, handlerMethod, loginUser);
      }
      return operation;
    };
  }

  /** 로그인이 필요한 API 설명 맨 앞에 사용권 발급 → Authorize 순서를 붙인다 (예시 실행 시 401 방지) */
  private static void addLoginGuide(Operation operation, boolean required) {
    String guide = required
        ? "🔒 **로그인 필요** · Test Account › `POST /claim` 으로 받은 토큰을 Authorize에 입력 (없으면 401)\n\n"
        : "🔓 로그인 선택 · 토큰을 넣으면 내 상태(`isMine` 등)가 함께 표시됩니다\n\n";
    String description = operation.getDescription();
    operation.setDescription(description == null || description.isBlank() ? guide.strip() : guide + description);
  }

  private static void markCreated(Operation operation, HandlerMethod handlerMethod) {
    ApiResponses responses = operation.getResponses();
    if (handlerMethod.hasMethodAnnotation(PostMapping.class)
        && ResponseEntity.class.isAssignableFrom(handlerMethod.getMethod().getReturnType())
        && responses != null && responses.containsKey("200") && !responses.containsKey("201")) {
      ApiResponse ok = responses.remove("200");
      responses.addApiResponse("201", ok.description("생성 성공 (COMMON201)"));
    }
  }

  private static void addErrorResponses(Operation operation, HandlerMethod handlerMethod, LoginUser loginUser) {
    boolean requiresUser = loginUser != null && loginUser.required();
    boolean hasPathVariable = Arrays.stream(handlerMethod.getMethodParameters())
        .anyMatch(p -> p.hasParameterAnnotation(org.springframework.web.bind.annotation.PathVariable.class));
    boolean mutatesOwned = requiresUser && hasPathVariable
        && (handlerMethod.hasMethodAnnotation(org.springframework.web.bind.annotation.PutMapping.class)
        || handlerMethod.hasMethodAnnotation(org.springframework.web.bind.annotation.PatchMapping.class)
        || handlerMethod.hasMethodAnnotation(org.springframework.web.bind.annotation.DeleteMapping.class));
    String domain = handlerMethod.getBeanType().getSimpleName();
    boolean testAccount = domain.equals("TestAccountController");
    String action = handlerMethod.getMethod().getName();
    boolean participationResult = domain.equals("ParticipationResultController");
    String notFoundCode = participationResult ? "PARTICIPATION404 · ROUTINE404" : testAccount ? "TEST404" : domain.contains("Comment") ? "COMMENT404 · ROUTINE404"
        : domain.contains("Routine") ? "ROUTINE404" : domain.contains("Image") ? "IMAGE404" : "USER404";
    boolean updatesProfile = domain.equals("UserController")
        && handlerMethod.hasMethodAnnotation(org.springframework.web.bind.annotation.PatchMapping.class);
    boolean changesParticipation = domain.equals("RoutineParticipationController");

    Map<String, ApiResponse> errors = new LinkedHashMap<>();
    errors.put("400", error("입력값 오류 (COMMON400 등) — result에 필드별 메시지", "COMMON400", "요청 형식이 올바르지 않습니다.",
        Map.of("필드명", "오류 메시지")));
    if (loginUser != null || (testAccount && (action.equals("heartbeat") || action.equals("release")))) {
      ErrorStatus invalidSession = ErrorStatus.TEST_SESSION_INVALID;
      errors.put("401", error(invalidSession.getMessage(), invalidSession.getCode(), invalidSession.getMessage(), null));
    }
    if (changesParticipation) {
      ErrorStatus forbidden = ErrorStatus.PARTICIPATION_OWNER_FORBIDDEN;
      errors.put("403", error(forbidden.getMessage(), forbidden.getCode(), forbidden.getMessage(), null));
    } else if (participationResult && mutatesOwned) {
      ErrorStatus forbidden = ErrorStatus.PARTICIPATION_RESULT_FORBIDDEN;
      errors.put("403", error(forbidden.getMessage(), forbidden.getCode(), forbidden.getMessage(), null));
    } else if (mutatesOwned) {
      errors.put("403", domain.contains("Comment")
          ? error("댓글 작성자가 아님", "COMMENT403", "댓글 작성자만 수정하거나 삭제할 수 있습니다.", null)
          : error("루틴 작성자가 아님", "ROUTINE403", "루틴 작성자만 수정하거나 삭제할 수 있습니다.", null));
    }
    if (testAccount && action.equals("register")) {
      ErrorStatus occupied = ErrorStatus.TEST_SLOT_OCCUPIED;
      errors.put("409", error("등록된 슬롯(TEST4092) 또는 중복 닉네임(USER4091)", occupied.getCode(), occupied.getMessage(), null));
    }
    if (testAccount && action.equals("claim")) {
      ErrorStatus inUse = ErrorStatus.TEST_ACCOUNT_IN_USE;
      errors.put("409", error(inUse.getMessage(), inUse.getCode(), inUse.getMessage(), null));
    }
    if (updatesProfile) {
      errors.put("409", error("중복된 닉네임", "USER4091", ErrorStatus.USER_DUPLICATE_NICKNAME.getMessage(), null));
    }
    if (changesParticipation && !handlerMethod.getMethod().getName().equals("join")) {
      ErrorStatus invalidState = ErrorStatus.PARTICIPATION_INVALID_STATE;
      errors.put("409", error(invalidState.getMessage(), invalidState.getCode(), invalidState.getMessage(), null));
    }
    if (loginUser != null || hasPathVariable) {
      errors.put("404", error("대상 없음 (" + notFoundCode + (updatesProfile ? " · 이미지 없음 IMAGE404" : "") + ")",
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
